import com.moon.uno.*;
import java.util.function.BooleanSupplier;

/** Four actual TCP clients, not mocked messages. */
public class LanCheck {
    static int checks=0;
    static void ok(boolean yes,String name){checks++;if(!yes)throw new AssertionError(name);}
    static void waitFor(BooleanSupplier condition)throws Exception{long end=System.currentTimeMillis()+7000;while(!condition.getAsBoolean()){if(System.currentTimeMillis()>end)throw new AssertionError("Timed out");Thread.sleep(10);}}
    static void act(LanRoom r,String command,int index,int color)throws Exception{r.action(command,index,color);waitFor(()->!r.pending||r.closed);ok(!r.closed,"connection remains alive");}
    public static void main(String[] args)throws Exception{
        LanRoom[] rooms=new LanRoom[4];LanRoom extra=null;
        try{
            rooms[0]=LanRoom.create("房主",new LanRoom.Listener(){public void changed(){}public void failed(String s){System.err.println(s);}});waitFor(()->rooms[0].connected);
            for(int i=1;i<4;i++){rooms[i]=LanRoom.join("127.0.0.1","牌友"+i,null);final int n=i;waitFor(()->rooms[n].connected);}
            waitFor(()->!rooms[0].seats[3].equals("空席"));
            extra=LanRoom.join("127.0.0.1","满房测试",null);final LanRoom full=extra;waitFor(()->full.closed);ok(full.status.contains("房间已满"),"full room refused");
            act(rooms[1],"START",0,0);ok(rooms[1].view==null,"guest cannot start");
            act(rooms[0],"START",0,0);waitFor(()->rooms[3].view!=null);
            for(LanRoom r:rooms){waitFor(()->r.view!=null);ok(r.view.hands.get(0).size()==7,"own hand dealt");for(int p=1;p<4;p++)for(UnoGame.Card c:r.view.hands.get(p))ok(c.color==-2&&c.value==-2,"opponent faces hidden");}
            int rev=rooms[1].revision;act(rooms[1],"DRAW",0,0);ok(rooms[1].revision==rev,"wrong turn refused");
            int old=rooms[0].revision;rooms[0].revision=old-1;act(rooms[0],"DRAW",0,0);ok(rooms[0].view.hands.get(0).size()==7&&rooms[0].revision==old,"stale action refused");
            act(rooms[0],"PLAY",999,0);ok(rooms[0].view.hands.get(0).size()==7,"bad index refused");
            extra=LanRoom.join("127.0.0.1","迟到",null);final LanRoom denied=extra;waitFor(()->denied.closed);ok(!denied.connected,"join during game refused");
            for(int step=0;step<3000&&rooms[0].view.winner<0;step++){
                int seat=rooms[0].view.turn;LanRoom r=rooms[seat];int current=rooms[0].revision;waitFor(()->r.revision>=current&&r.view.turn==0);UnoGame g=r.view;
                if(g.hands.get(0).size()==2){act(r,"UNO",0,0);g=r.view;}
                int index=-1;for(int i=0;i<g.hands.get(0).size();i++)if(g.canPlay(g.hands.get(0).get(i))){index=i;break;}
                int before=r.revision;act(r,index<0?"DRAW":"PLAY",index,step%4);waitFor(()->rooms[0].revision>before);
            }
            ok(rooms[0].view.winner>=0,"complete four-client game");int winner=rooms[0].view.winner;int finalRev=rooms[0].revision;
            for(int i=0;i<4;i++){final LanRoom r=rooms[i];waitFor(()->r.revision==finalRev);ok(r.view.winner==(winner-i+4)%4,"same winner across seats");}
            act(rooms[0],"START",0,0);waitFor(()->rooms[0].view.winner<0);ok(rooms[0].view.hands.get(0).size()==7,"rematch");
            rooms[1].close();waitFor(()->rooms[0].view.names[1].endsWith("·AI"));ok(true,"disconnect becomes AI");
            rooms[0].close();waitFor(()->rooms[2].closed&&rooms[3].closed);ok(true,"host exit closes room");
        }finally{for(LanRoom r:rooms)if(r!=null)r.close();if(extra!=null)extra.close();}
        // Two humans can begin with two AI seats.
        LanRoom host=null,guest=null;try{host=LanRoom.create("双人房主",null);final LanRoom h=host;waitFor(()->h.connected);guest=LanRoom.join("127.0.0.1","双人客人",null);final LanRoom g=guest;waitFor(()->g.connected);waitFor(()->!h.seats[1].equals("空席"));act(h,"START",0,0);waitFor(()->g.view!=null);ok(h.view.names[2].endsWith("·AI")&&h.view.names[3].endsWith("·AI"),"empty seats filled by AI");}finally{if(host!=null)host.close();if(guest!=null)guest.close();}
        System.out.println("PASS LAN: "+checks+" checks; real TCP four-player match, privacy, validation, rematch, disconnect, two-player room");
    }
}
