package com.moon.uno;

import android.content.Context;
import android.graphics.*;
import android.provider.Settings;
import android.view.View;

/** Small procedural illustrations; all interface text and controls remain native. */
final class MoonArt {
    static final int GOLD=0xffdfc591, IVORY=0xfff5ead6;
    static boolean motion(Context c){try{return Settings.Global.getFloat(c.getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1f)>0;}catch(Exception e){return true;}}
    static void card(Canvas c,Paint p,float x,float y,float w,float h,int color,String label,boolean back){
        p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(x+w*.5f,y+h*.7f,w*.75f,new int[]{0x66030a10,0x00030a10},null,Shader.TileMode.CLAMP));c.drawOval(x-4,y+h*.72f,x+w+7,y+h+12,p);p.setShader(null);p.setColor(0x55040d15);c.drawRoundRect(x+2,y+5,x+w+2,y+h+6,10,10,p);
        p.setColor(color);p.setShader(new LinearGradient(x,y,x+w,y+h,new int[]{color,shade(color,.7f)},null,Shader.TileMode.CLAMP));c.drawRoundRect(x,y,x+w,y+h,10,10,p);p.setShader(null);
        p.setShader(new LinearGradient(x,y,x+w*.65f,y+h*.45f,new int[]{0x33ffffff,0x00ffffff},null,Shader.TileMode.CLAMP));c.drawRoundRect(x+1,y+1,x+w-1,y+h-1,10,10,p);p.setShader(null);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(back?GOLD:0xffeedec2);c.drawRoundRect(x+1,y+1,x+w-1,y+h-1,10,10,p);
        p.setColor(0x55eedec2);c.drawRoundRect(x+5,y+5,x+w-5,y+h-5,6,6,p);
        c.save();c.rotate(24,x+w/2,y+h/2);c.drawOval(x+w*.17f,y+h*.18f,x+w*.83f,y+h*.82f,p);c.restore();
        p.setStyle(Paint.Style.FILL);p.setColor(IVORY);p.setTypeface(Typeface.create("sans-serif-condensed",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(w*(back?.26f:.43f));c.drawText(label,x+w/2,y+h*.59f,p);
        p.setTextSize(w*.17f);p.setTextAlign(Paint.Align.LEFT);c.drawText(back?"✧":label,x+w*.12f,y+h*.18f,p);
        if(!back){c.save();c.rotate(180,x+w/2,y+h/2);c.drawText(label,x+w*.12f,y+h*.18f,p);c.restore();}
    }
    static int shade(int color,float f){return Color.rgb((int)(Color.red(color)*f),(int)(Color.green(color)*f),(int)(Color.blue(color)*f));}
    static final class Scene extends View {
        final Paint p=new Paint(3); final Path path=new Path(); Shader sky;int width,height;long origin=android.os.SystemClock.uptimeMillis();
        Scene(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)invalidate();}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility==VISIBLE)invalidate();}
        @Override protected void onDraw(Canvas c){int w=getWidth(),h=getHeight();if(w!=width||h!=height){width=w;height=h;sky=new LinearGradient(0,0,w,h,new int[]{0xff0b1824,0xff18363f,0xff0b1926},null,Shader.TileMode.CLAMP);}float t=motion(getContext())?(android.os.SystemClock.uptimeMillis()-origin)/1000f:0;
            p.setAlpha(255);p.setShader(sky);c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setShader(new RadialGradient(w*.85f,h*.16f,w*.6f,new int[]{0x224bb4b4,0x004bb4b4},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h*.6f,p);p.setShader(null);
            for(int layer=0;layer<3;layer++){path.reset();path.moveTo(0,h);path.lineTo(0,h*(.53f+layer*.12f));for(int i=0;i<=10;i++)path.lineTo(w*i/9f,h*(.52f+layer*.12f)+(float)Math.sin(i*1.7+layer)*h*.035f);path.lineTo(w,h);path.close();p.setColor(new int[]{0x225a8987,0x33385964,0x770a1824}[layer]);c.drawPath(path,p);}
            for(int i=0;i<22;i++){float x=((i*137%997)/997f*w+(float)Math.sin(t*.16+i)*9),y=((i*233%991)/991f*h-t*(1+i%3))%h;if(y<0)y+=h;p.setColor(Color.argb((int)(20+22*(1+Math.sin(t*.55+i))),221,195,146));float radius=Math.max(1,w/450f)*(i%5==0?1.8f:.65f);c.drawCircle(x,y,radius,p);if(i%7==0){p.setStrokeWidth(.7f);c.drawLine(x-radius*2,y,x+radius*2,y,p);c.drawLine(x,y-radius*2,x,y+radius*2,p);}}
            // Cherry branch stays outside the main reading column.
            p.setColor(0x445b6c72);p.setStrokeWidth(2);c.drawLine(w*.8f,0,w,h*.15f,p);for(int i=0;i<5;i++){float x=w*(.84f+i*.035f),y=h*(.026f+i*.025f);p.setColor(0x338f8b8f);for(int j=0;j<5;j++){double a=j*Math.PI*2/5;c.drawCircle(x+(float)Math.cos(a)*5,y+(float)Math.sin(a)*5,3,p);}}
            if(hasWindowFocus()&&isShown()&&motion(getContext()))postInvalidateDelayed(33);
        }
    }
    static final class Hero extends View {
        final Paint p=new Paint(3);Hero(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas real){float scale=Math.min(getWidth()/360f,getHeight()/232f);if(scale<=0)return;real.save();real.translate((getWidth()-360*scale)/2,(getHeight()-232*scale)/2);real.scale(scale,scale);Canvas c=real;float w=360,h=204;
            p.setShader(new RadialGradient(180,h*.46f,145,new int[]{0x384b8b88,0x004b8b88},null,Shader.TileMode.CLAMP));c.drawRect(0,0,w,h,p);p.setShader(null);
            p.setColor(0xffd8cda6);c.drawCircle(211,70,45,p);p.setColor(0x297b887f);c.drawCircle(198,58,12,p);c.drawCircle(230,76,17,p);c.drawCircle(204,92,8,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.6f);p.setColor(0x55dfc591);c.drawOval(46,19,313,h-10,p);p.setStyle(Paint.Style.FILL);
            for(int i=0;i<5;i++){float cx=180+(i-2)*37,cy=h*.61f+Math.abs(i-2)*9;c.save();c.rotate((i-2)*13,cx,cy+45);int[] colors={0xff347761,0xffab823d,0xffa7403c,0xff366e8b,0xff1b303f};card(c,p,cx-35,cy-49,70,104,colors[i],new String[]{"+2","7","1","⇄","UNO"}[i],i==4);c.restore();}
            p.setColor(GOLD);p.setTextSize(13);p.setTextAlign(Paint.Align.CENTER);c.drawText("✧",56,50,p);c.drawText("✧",310,144,p);
            // Small suspended lantern.
            p.setColor(0xff8e5144);p.setStrokeWidth(1);c.drawLine(49,0,49,39,p);c.drawRoundRect(39,39,59,68,8,8,p);p.setColor(GOLD);c.drawRect(43,39,55,42,p);c.drawRect(43,65,55,68,p);c.drawLine(49,68,49,85,p);
            real.restore();
        }
    }
    static final class Table extends View {
        final Paint p=new Paint(3);Table(Context c){super(c);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float w=getWidth(),h=getHeight();p.setShader(new RadialGradient(w/2,h*.48f,w*.7f,new int[]{0xff2b5555,0xff193b42,0xff102936},null,Shader.TileMode.CLAMP));c.drawRoundRect(1,1,w-1,h-1,28,28,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setColor(0x998c927c);p.setStrokeWidth(1);c.drawRoundRect(1,1,w-1,h-1,28,28,p);p.setColor(0x448c927c);c.drawRoundRect(8,8,w-8,h-8,23,23,p);p.setColor(0x227fc3b1);p.setStrokeWidth(Math.max(1,w/240f));c.drawOval(w*.14f,h*.1f,w*.86f,h*.9f,p);c.drawOval(w*.26f,h*.2f,w*.74f,h*.8f,p);p.setStyle(Paint.Style.FILL);p.setShader(new RadialGradient(w/2,h/2,Math.min(w,h)*.38f,new int[]{0x334ec2ac,0x004ec2ac},null,Shader.TileMode.CLAMP));c.drawCircle(w/2,h/2,Math.min(w,h)*.38f,p);p.setShader(null);for(int i=0;i<8;i++){double a=i*Math.PI/4;float x=w/2+(float)Math.cos(a)*w*.37f,y=h/2+(float)Math.sin(a)*h*.37f;p.setColor(i%2==0?0x66dfc591:0x337fc3b1);c.drawCircle(x,y,Math.max(1,w/260f),p);}}
    }
}
