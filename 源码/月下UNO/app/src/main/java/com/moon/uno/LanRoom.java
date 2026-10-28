package com.moon.uno;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/** TCP room. The host owns rules; clients receive only their own card faces. */
public final class LanRoom implements Closeable {
    public static final int PORT=38571, PROTOCOL=2;
    public interface Listener { void changed(); void failed(String reason); }
    public volatile Listener listener;
    public volatile UnoGame view;
    public volatile String[] seats={"连接中…","空席","空席","空席"};
    public volatile String status="正在连接…";
    public volatile boolean connected=false, host=false, closed=false, pending=false;
    public volatile int revision=0;
    private volatile Socket socket; private DataOutputStream out;
    private volatile Server server;
    private final ExecutorService writes=Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService heartbeat=Executors.newSingleThreadScheduledExecutor();
    private LanRoom(){}
    public static LanRoom create(String name,Listener listener){LanRoom r=new LanRoom();r.host=true;r.listener=listener;new Thread(()->{try{r.server=new Server();if(r.closed){r.server.close();return;}r.connect("127.0.0.1",name);}catch(IOException e){r.fail("创建失败：端口被占用或网络不可用（"+e.getMessage()+"）");}},"uno-host").start();return r;}
    public static LanRoom join(String ip,String name,Listener listener){LanRoom r=new LanRoom();r.listener=listener;new Thread(()->{try{r.connect(ip,name);}catch(IOException e){r.fail("无法连接房主，请检查地址、同一 Wi-Fi 和热点隔离设置");}},"uno-join").start();return r;}
    private void connect(String ip,String name)throws IOException {
        if(closed)return;Socket s=new Socket();socket=s;s.connect(new InetSocketAddress(ip,PORT),5000);s.setTcpNoDelay(true);s.setSoTimeout(15000);out=new DataOutputStream(s.getOutputStream());out.writeInt(PROTOCOL);out.writeUTF(clean(name));out.writeUTF(host?server.key:"");out.flush();
        if(closed){s.close();return;}
        try{heartbeat.scheduleAtFixedRate(()->send("PING",-1,0),3,3,TimeUnit.SECONDS);}catch(RejectedExecutionException e){s.close();return;}
        try(DataInputStream in=new DataInputStream(s.getInputStream())){while(!closed){String type=in.readUTF();if(type.equals("ERROR")){throw new IOException(in.readUTF());}if(type.equals("PONG"))continue;if(!type.equals("STATE"))throw new IOException("协议不兼容");
            int rev=in.readInt();boolean started=in.readBoolean();String message=in.readUTF();String[] next=new String[4];for(int i=0;i<4;i++)next[i]=in.readUTF();
            UnoGame g=null;if(started){g=new UnoGame(false);g.message=message;g.turn=in.readInt();g.direction=in.readInt();g.winner=in.readInt();g.color=in.readInt();g.top=new UnoGame.Card(in.readInt(),in.readInt());g.calledUno=in.readBoolean();for(int i=0;i<4;i++){g.names[i]=next[i];int count=in.readInt();for(int j=0;j<count;j++)g.hands.get(i).add(i==0?new UnoGame.Card(in.readInt(),in.readInt()):new UnoGame.Card(-2,-2));}}
            revision=rev;seats=next;view=g;status=message;connected=true;pending=false;changed();
        }}catch(IOException e){if(!closed)fail("连接已断开："+e.getMessage());}
    }
    public void action(String command,int index,int color){if(pending||!connected||closed)return;pending=true;changed();send(command,index,color);}
    private void send(String command,int index,int color){if(closed)return;final int rev=revision;try{writes.execute(()->{try{if(out==null)return;out.writeUTF(command);out.writeInt(rev);out.writeInt(index);out.writeInt(color);out.flush();}catch(IOException e){fail("连接已断开，请返回大厅重新加入");}});}catch(RejectedExecutionException ignored){}}
    private void changed(){Listener l=listener;if(l!=null)l.changed();}
    private void fail(String reason){if(closed)return;status=reason;close();Listener l=listener;if(l!=null)l.failed(reason);}
    public void close(){closed=true;connected=false;heartbeat.shutdownNow();writes.shutdownNow();try{if(socket!=null)socket.close();}catch(IOException ignored){}if(server!=null)server.close();}
    static String clean(String s){if(s==null||s.trim().isEmpty())return "旅人";s=s.trim().replaceAll("[\\p{Cntrl}]","");return s.substring(0,Math.min(s.length(),8));}
    public static String addresses(){StringBuilder b=new StringBuilder();try{Enumeration<NetworkInterface> it=NetworkInterface.getNetworkInterfaces();while(it.hasMoreElements()){NetworkInterface n=it.nextElement();if(!n.isUp()||n.isLoopback())continue;Enumeration<InetAddress> a=n.getInetAddresses();while(a.hasMoreElements()){InetAddress ip=a.nextElement();if(ip instanceof Inet4Address&&!ip.isLoopbackAddress()){if(b.length()>0)b.append(" / ");b.append(ip.getHostAddress());}}}}catch(Exception ignored){}return b.length()==0?"未找到 IPv4 地址，请先连接 Wi-Fi 或开启热点":b.toString();}

    static final class Server implements Closeable {
        final String key=UUID.randomUUID().toString(); final ServerSocket listener; final Peer[] peers=new Peer[4]; final ScheduledExecutorService bots=Executors.newSingleThreadScheduledExecutor();
        UnoGame game;int revision=0;volatile boolean closed=false;
        Server()throws IOException{listener=new ServerSocket();listener.setReuseAddress(true);listener.bind(new InetSocketAddress(PORT));new Thread(()->{while(!closed){try{Socket s=listener.accept();s.setSoTimeout(15000);s.setTcpNoDelay(true);new Thread(()->accept(s),"uno-peer").start();}catch(IOException e){if(!closed)close();}}},"uno-accept").start();bots.scheduleWithFixedDelay(()->{synchronized(this){if(game!=null&&game.winner<0&&!game.human[game.turn]){game.aiStep();revision++;broadcast();}}},1,1,TimeUnit.SECONDS);}
        void accept(Socket s){Peer peer=null;try{DataInputStream in=new DataInputStream(s.getInputStream());DataOutputStream out=new DataOutputStream(s.getOutputStream());int version=in.readInt();String name=in.readUTF();String token=in.readUTF();synchronized(this){if(closed){s.close();return;}if(version!=PROTOCOL||game!=null){out.writeUTF("ERROR");out.writeUTF(version!=PROTOCOL?"版本不兼容，请全员安装相同版本":"牌局已开始，暂不支持中途加入");out.flush();s.close();return;}int seat=token.equals(key)?0:1;while(seat<4&&peers[seat]!=null)seat++;if(seat==4){out.writeUTF("ERROR");out.writeUTF("房间已满");out.flush();s.close();return;}peer=new Peer(s,out,seat,clean(name));peers[seat]=peer;revision++;broadcast();}
                while(!closed){String cmd=in.readUTF();int rev=in.readInt(),index=in.readInt(),color=in.readInt();synchronized(this){if(cmd.equals("PING")){ByteArrayOutputStream pong=new ByteArrayOutputStream();new DataOutputStream(pong).writeUTF("PONG");peer.send(pong.toByteArray());continue;}act(peer,cmd,rev,index,color);}}
            }catch(IOException ignored){}finally{synchronized(this){if(peer!=null&&peers[peer.seat]==peer){peers[peer.seat]=null;peer.writer.shutdownNow();if(peer.seat==0){close();}else{if(game!=null){game.human[peer.seat]=false;game.names[peer.seat]+="·AI";game.message="玩家断线，已由 AI 接管";if(game.turn==peer.seat)game.calledUno=false;}revision++;broadcast();}}}try{s.close();}catch(IOException ignored){}}}
        synchronized void act(Peer p,String cmd,int rev,int index,int color){
            if(rev!=revision){sendState(p);return;}
            if(cmd.equals("START")){if(p.seat!=0||game!=null&&game.winner<0){sendState(p);return;}int count=0;for(Peer peer:peers)if(peer!=null)count++;if(count<2){sendState(p);return;}game=new UnoGame();for(int i=0;i<4;i++){game.human[i]=peers[i]!=null;game.names[i]=peers[i]==null?UnoGame.NAMES[i]+"·AI":peers[i].name;}game.message="联机开局 · "+game.names[0]+" 先手";revision++;broadcast();return;}
            if(game==null||game.winner>=0||p.seat!=game.turn){sendState(p);return;}
            boolean changed=false;if(cmd.equals("PLAY"))changed=game.play(index,color);else if(cmd.equals("DRAW")){game.drawAndPass();changed=true;}else if(cmd.equals("UNO")&&game.hands.get(p.seat).size()==2){game.calledUno=true;changed=true;}
            if(changed){revision++;broadcast();}else sendState(p);
        }
        void broadcast(){for(Peer p:peers)if(p!=null)sendState(p);}
        void sendState(Peer p){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeUTF("STATE");o.writeInt(revision);o.writeBoolean(game!=null);o.writeUTF(game==null?"等待牌友入席 · 空席开局后由 AI 补位":game.message);for(int i=0;i<4;i++){int seat=(p.seat+i)%4;o.writeUTF(game==null?(peers[seat]==null?"空席":peers[seat].name):game.names[seat]);}if(game!=null){o.writeInt((game.turn-p.seat+4)%4);o.writeInt(game.direction);o.writeInt(game.winner<0?-1:(game.winner-p.seat+4)%4);o.writeInt(game.color);o.writeInt(game.top.color);o.writeInt(game.top.value);o.writeBoolean(game.turn==p.seat&&game.calledUno);for(int i=0;i<4;i++){java.util.List<UnoGame.Card> hand=game.hands.get((p.seat+i)%4);o.writeInt(hand.size());if(i==0)for(UnoGame.Card c:hand){o.writeInt(c.color);o.writeInt(c.value);}}}o.flush();p.send(bytes.toByteArray());}catch(IOException e){try{p.socket.close();}catch(IOException ignored){}}}
        public synchronized void close(){if(closed)return;closed=true;bots.shutdownNow();try{listener.close();}catch(IOException ignored){}for(Peer p:peers)if(p!=null)try{p.writer.shutdownNow();p.socket.close();}catch(IOException ignored){}}
    }
    static final class Peer {final Socket socket;final DataOutputStream out;final int seat;final String name;final ThreadPoolExecutor writer=new ThreadPoolExecutor(1,1,0,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(8));void send(byte[] bytes){try{writer.execute(()->{try{out.write(bytes);out.flush();}catch(IOException e){try{socket.close();}catch(IOException ignored){}}});}catch(RejectedExecutionException e){try{socket.close();}catch(IOException ignored){}}}Peer(Socket s,DataOutputStream o,int seat,String name){socket=s;out=o;this.seat=seat;this.name=name;}}
}
