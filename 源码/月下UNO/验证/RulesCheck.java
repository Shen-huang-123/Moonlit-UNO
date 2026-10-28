import com.moon.uno.UnoGame;
public class RulesCheck {
    static int checks=0;
    static void ok(boolean value,String name){checks++;if(!value)throw new AssertionError(name);}
    static UnoGame scenario(int value){UnoGame g=new UnoGame();for(int i=0;i<4;i++){g.hands.get(i).clear();g.hands.get(i).add(new UnoGame.Card(1,8));g.hands.get(i).add(new UnoGame.Card(2,9));}g.hands.get(0).add(0,new UnoGame.Card(0,value));g.color=0;g.top=new UnoGame.Card(0,3);return g;}
    public static void main(String[] args){
        UnoGame g=new UnoGame();for(int i=0;i<4;i++)ok(g.hands.get(i).size()==7,"deal");ok(g.top.value<10,"numeric opening");
        g=scenario(7);ok(g.play(0,0)&&g.turn==1,"normal turn");
        g=scenario(10);g.play(0,0);ok(g.turn==2,"skip");
        g=scenario(11);g.play(0,0);ok(g.turn==3&&g.direction==-1,"reverse");
        g=scenario(12);g.play(0,0);ok(g.turn==2&&g.hands.get(1).size()==4,"draw two");
        g=scenario(7);ok(!g.play(1,0)&&g.turn==0,"illegal color");
        g=scenario(7);g.hands.get(0).clear();g.hands.get(0).add(new UnoGame.Card(-1,14));g.hands.get(0).add(new UnoGame.Card(0,9));ok(!g.canPlay(g.hands.get(0).get(0)),"wild four restriction");g.hands.get(0).set(1,new UnoGame.Card(1,9));g.calledUno=true;g.play(0,2);ok(g.color==2&&g.hands.get(1).size()==6&&g.turn==2,"wild four effect");
        g=scenario(7);g.hands.get(0).remove(2);g.play(0,0);ok(g.hands.get(0).size()==3,"missed uno penalty");
        g=scenario(7);g.hands.get(0).remove(2);g.calledUno=true;g.play(0,0);ok(g.hands.get(0).size()==1,"called uno");
        g=scenario(7);g.turn=2;g.human[2]=true;g.hands.get(2).set(0,new UnoGame.Card(0,7));g.play(0,0);ok(g.hands.get(2).size()==3,"network guest missed uno penalty");
        g=scenario(7);g.turn=3;g.human[3]=true;g.calledUno=true;g.hands.get(3).set(0,new UnoGame.Card(0,7));g.play(0,0);ok(g.hands.get(3).size()==1,"network guest called uno");
        g=scenario(7);g.hands.get(0).clear();g.hands.get(0).add(new UnoGame.Card(0,7));g.play(0,0);ok(g.winner==0,"winner");
        for(int run=0;run<30;run++){g=new UnoGame();int steps=0;while(g.winner<0&&steps++<5000){if(g.turn==0){g.calledUno=true;int i;for(i=0;i<g.hands.get(0).size();i++)if(g.canPlay(g.hands.get(0).get(i))){g.play(i,1);break;}if(i==g.hands.get(0).size()&&g.turn==0&&g.winner<0)g.drawAndPass();}else g.aiStep();}ok(g.winner>=0,"complete simulation");}
        System.out.println("PASS: "+checks+" checks, including 30 complete games");
    }
}
