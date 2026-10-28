package com.moon.uno;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;

/** Checks actual measured views and touch behavior at the emulator's current size. */
public class UiProbe extends Instrumentation {
    MainActivity activity;int checks=0;StringBuilder report=new StringBuilder();
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    void ok(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    void onUi(Runnable run){runOnMainSync(run);waitForIdleSync();}
    void bounds(View view,Rect safe){
        if(view.getVisibility()!=View.VISIBLE)return;
        Rect actual=new Rect();view.getGlobalVisibleRect(actual);int[] xy=new int[2];view.getLocationOnScreen(xy);
        ok(view.getHeight()>0&&view.getWidth()>0,"zero view "+view.getClass().getSimpleName());
        ok(xy[0]>=safe.left-1&&xy[1]>=safe.top-1&&xy[0]+view.getWidth()<=safe.right+1&&xy[1]+view.getHeight()<=safe.bottom+1,"outside safe region "+view.getClass().getSimpleName()+" "+new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight())+" vs "+safe);
        if(view instanceof HorizontalScrollView)return;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)bounds(group.getChildAt(i),safe);}
    }
    void inspect(String page){onUi(()->{int[] xy=new int[2];activity.shell.getLocationOnScreen(xy);Rect safe=new Rect(xy[0]+activity.shell.getPaddingLeft(),xy[1]+activity.shell.getPaddingTop(),xy[0]+activity.shell.getWidth()-activity.shell.getPaddingRight(),xy[1]+activity.shell.getHeight()-activity.shell.getPaddingBottom());ok(safe.height()>0&&safe.width()>0,"safe region visible");for(int i=0;i<activity.root.getChildCount();i++)bounds(activity.root.getChildAt(i),safe);report.append(page).append(" ").append(safe).append("\n");});}
    Button find(View view,String text){if(view instanceof Button&&((Button)view).getText().toString().contains(text))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button b=find(group.getChildAt(i),text);if(b!=null)return b;}}return null;}
    void touch(View view){final int[] point=new int[2];onUi(()->{view.getLocationOnScreen(point);point[0]+=view.getWidth()/2;point[1]+=view.getHeight()/2;});long time=SystemClock.uptimeMillis();sendPointerSync(MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,point[0],point[1],0));sendPointerSync(MotionEvent.obtain(time,time+80,MotionEvent.ACTION_UP,point[0],point[1],0));waitForIdleSync();}
    @Override public void onStart(){Bundle result=new Bundle();LanRoom guest=null;try{
        Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);activity=(MainActivity)startActivitySync(intent);waitForIdleSync();inspect("home");
        onUi(()->activity.start());inspect("game");
        Button draw=find(activity.root,"摸");ok(draw!=null&&draw.isEnabled(),"draw is available");Button uno=find(activity.root,"UNO");ok(uno!=null,"UNO exists");int before=activity.game.hands.get(0).size();touch(draw);onUi(()->{ok(activity.game.hands.get(0).size()==before+1,"actual touch draws a card");activity.handler.removeCallbacksAndMessages(null);});inspect("after draw");
        onUi(()->{activity.home();activity.lan=LanRoom.create("测试房主",activity.roomListener);activity.lobby();});long end=SystemClock.uptimeMillis()+6000;while(!activity.lan.connected&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);ok(activity.lan.connected,"host connected");waitForIdleSync();inspect("room");
        guest=LanRoom.join("127.0.0.1","测试牌友",null);end=SystemClock.uptimeMillis()+6000;while(!guest.connected&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);ok(guest.connected,"guest connected");waitForIdleSync();inspect("room with guest");
        onUi(()->activity.lan.action("START",0,0));end=SystemClock.uptimeMillis()+6000;while(activity.lan.view==null&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);waitForIdleSync();inspect("network game");ok(activity.lan.view!=null,"network game started");
        result.putString("stream","PASS "+checks+" measured-view checks\n"+report);finish(Activity.RESULT_OK,result);
    }catch(Throwable e){result.putString("stream","FAIL "+e+"\n"+report);finish(Activity.RESULT_CANCELED,result);}finally{if(guest!=null)guest.close();if(activity!=null)onUi(()->{activity.home();activity.finish();});}}
}
