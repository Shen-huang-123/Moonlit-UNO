import com.moon.uno.*;
/** Use adb forward tcp:38571 tcp:38571 after creating a room on Android. */
public class AndroidLanGuest {
    public static void main(String[] args)throws Exception{
        LanRoom r=LanRoom.join("127.0.0.1","联机验收",null);
        try{long end=System.currentTimeMillis()+90000;boolean acted=false;while(System.currentTimeMillis()<end){if(r.closed)throw new AssertionError(r.status);if(r.view!=null&&r.view.turn==0&&!r.pending&&!acted){r.action("DRAW",0,0);acted=true;System.out.println("Android host -> guest: received own turn and submitted DRAW");}if(acted&&!r.pending){if(r.view.hands.get(0).size()!=8)throw new AssertionError("Guest draw not applied");System.out.println("PASS Android host / JVM guest: authoritative hand=8, AI seats="+r.view.names[1]+", "+r.view.names[2]);Thread.sleep(10000);return;}Thread.sleep(50);}throw new AssertionError("Android interaction timed out");}finally{r.close();}
    }
}
