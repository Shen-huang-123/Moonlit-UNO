package com.moon.uno;

import java.util.*;

/** Standalone rules engine: no Android dependency. No stacking or +4 challenge in this prototype. */
public final class UnoGame {
    public static final String[] NAMES = {"你", "青岚", "朱雀", "白藏"};
    public static final String[] COLORS = {"朱砂", "青玉", "黛蓝", "赭金"};
    public static final class Card {
        public final int color, value;
        public Card(int c,int v) { color=c; value=v; }
        public String label() { return value<10?""+value:value==10?"⊘":value==11?"⇄":value==12?"+2":value==13?"✦":"+4"; }
    }
    public final List<List<Card>> hands = new ArrayList<>();
    public final String[] names = NAMES.clone();
    public final boolean[] human = {true,false,false,false};
    private final List<Card> deck = new ArrayList<>(), discards = new ArrayList<>();
    public Card top;
    public int color, turn=0, direction=1, winner=-1;
    public boolean calledUno=false;
    public String message="月下开局，轮到你出牌";
    public UnoGame() { this(true); }
    public UnoGame(boolean deal) {
        for(int i=0;i<4;i++) hands.add(new ArrayList<>());
        if(!deal)return;
        for(int c=0;c<4;c++) { deck.add(new Card(c,0)); for(int v=1;v<=12;v++) for(int n=0;n<2;n++) deck.add(new Card(c,v)); }
        for(int i=0;i<4;i++) {deck.add(new Card(-1,13));deck.add(new Card(-1,14));}
        Collections.shuffle(deck);
        for(int n=0;n<7;n++) for(int p=0;p<4;p++) draw(p);
        int i=0; while(deck.get(i).value>9) i++;
        top=deck.remove(i);color=top.color;discards.add(top);
    }
    private void draw(int player) {
        if(deck.isEmpty()) { Card last=discards.remove(discards.size()-1);deck.addAll(discards);discards.clear();discards.add(last);Collections.shuffle(deck); }
        if(!deck.isEmpty()) hands.get(player).add(deck.remove(deck.size()-1));
    }
    public boolean canPlay(Card card) {
        if(card.value==14) { for(Card c:hands.get(turn)) if(c.color==color)return false; return true; }
        return card.color<0 || card.color==color || card.value==top.value;
    }
    public boolean play(int index,int chosenColor) {
        if(winner>=0 || index<0 || index>=hands.get(turn).size())return false;
        Card card=hands.get(turn).get(index);
        if(!canPlay(card) || (card.color<0 && (chosenColor<0 || chosenColor>3)))return false;
        int player=turn;
        hands.get(player).remove(index);top=card;discards.add(card);color=card.color<0?chosenColor:card.color;
        message=names[player]+"打出 "+COLORS[color]+" "+card.label();
        if(hands.get(player).isEmpty()) {winner=player;return true;}
        if(human[player] && hands.get(player).size()==1 && !calledUno) {draw(player);draw(player);message+=" · 漏喊 UNO，罚摸 2 张";}
        calledUno=false;
        if(card.value==11)direction=-direction;
        turn=(turn+direction+4)%4;
        if(card.value==10 || card.value==12 || card.value==14) {
            int count=card.value==12?2:card.value==14?4:0;
            for(int n=0;n<count;n++)draw(turn);
            if(count>0)message+=" · "+names[turn]+"摸 "+count+" 张";
            turn=(turn+direction+4)%4;
        }
        return true;
    }
    public void drawAndPass() {
        if(winner>=0)return;
        draw(turn);message=names[turn]+"摸 1 张，结束回合";calledUno=false;turn=(turn+direction+4)%4;
    }
    public void aiStep() {
        if(turn==0 || winner>=0)return;
        for(int i=0;i<hands.get(turn).size();i++) if(canPlay(hands.get(turn).get(i))) {
            int[] counts=new int[4];for(Card c:hands.get(turn))if(c.color>=0)counts[c.color]++;
            int best=0;for(int j=1;j<4;j++)if(counts[j]>counts[best])best=j;
            play(i,best);return;
        }
        drawAndPass();
    }
}
