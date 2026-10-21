package dev.nightfall.ui;

import dev.nightfall.MusicManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public final class MediaPlayerScreen extends Screen {
    public MediaPlayerScreen() { super(Text.literal("NIGHTFALL Media")); }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int w=width,h=height,cx=w/2;
        ctx.fill(0,0,w,h,0xB9091014);
        int pw=Math.min(420,w-30),ph=236,x=cx-pw/2,y=h/2-ph/2;
        ctx.fill(x+3,y+4,x+pw+3,y+ph+4,0x66000000);
        ctx.fill(x,y,x+pw,y+ph,0xF1162027);
        ctx.fill(x,y,x+pw,y+1,0xFF35434B);
        ctx.fill(x,y,x+3,y+ph,0xFF79BDB5);
        draw(ctx,"NIGHTFALL  /  MEDIA",x+20,y+17,0xFF79BDB5);
        draw(ctx,"Spotify и Яндекс Музыка через медиасессию Windows",x+20,y+35,0xFF8A9AA2);
        var state=MusicManager.state();
        String title=state.title().isBlank()?"Нет активного медиаплеера":state.title();
        String artist=state.artist().isBlank()?state.status():state.artist();
        draw(ctx,fit(title,pw-40),x+20,y+65,0xFFE1E8EA);
        draw(ctx,fit(artist,pw-40),x+20,y+83,0xFF9DADB3);
        String app=state.app().isBlank()?"SYSTEM MEDIA":state.app();
        draw(ctx,fit(app,pw-40),x+20,y+101,0xFF617780);
        int barX=x+20,barY=y+126,barW=pw-40;
        ctx.fill(barX,barY,barX+barW,barY+3,0xFF34434B);
        if(state.durationMs()>0){
            int filled=(int)(barW*Math.max(0,Math.min(1,state.positionNow()/(double)state.durationMs())));
            ctx.fill(barX,barY,barX+filled,barY+3,0xFF79BDB5);
        }
        String lyric=MusicManager.currentLyric();
        draw(ctx,fit(lyric.isBlank()?"Lyrics: "+(MusicManager.lyricError().isBlank()?"ищем синхронный текст…":MusicManager.lyricError()):lyric,pw-40),x+20,y+145,lyric.isBlank()?0xFF73858D:0xFFBFDAD6);
        int gap=8,bw=(pw-40-gap*2)/3,bh=31,by=y+174,bx=x+20;
        button(ctx,bx,by,bw,bh,"⏮ ПРЕДЫДУЩИЙ",mx,my,()->MusicManager.command("previous"));
        button(ctx,bx+bw+gap,by,bw,bh,state.playing()?"Ⅱ ПАУЗА":"▶ ПЛЕЙ",mx,my,()->MusicManager.command("toggle"));
        button(ctx,bx+(bw+gap)*2,by,bw,bh,"СЛЕДУЮЩИЙ ⏭",mx,my,()->MusicManager.command("next"));
        draw(ctx,"K — открыть плеер     ESC — закрыть",x+20,y+ph-18,0xFF60717A);
    }
    private String fit(String text,int maxWidth){
        if(text==null||text.isBlank())return "";
        String s=text;
        while(!s.isEmpty()&&textRenderer.getWidth(UiText.of(s))>maxWidth)s=s.substring(0,s.length()-1);
        return s.equals(text)?s:(s.length()>1?s.substring(0,s.length()-1)+"…":"…");
    }
    private void draw(DrawContext ctx,String text,int x,int y,int color){ctx.drawTextWithShadow(textRenderer,UiText.of(text),x,y,color);}
    private void button(DrawContext ctx,int x,int y,int w,int h,String label,int mx,int my,Runnable action){
        boolean hover=mx>=x&&mx<=x+w&&my>=y&&my<=y+h;
        ctx.fill(x,y,x+w,y+h,hover?0xFF30434A:0xFF202D35);
        ctx.fill(x,y,x+w,y+1,hover?0xFF79BDB5:0xFF35434B);
        int tw=textRenderer.getWidth(UiText.of(label));
        draw(ctx,label,x+(w-tw)/2,y+(h-8)/2,hover?0xFFE1F0ED:0xFFB9C6CA);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0){
            int pw=Math.min(420,width-30),ph=236,x=width/2-pw/2,y=height/2-ph/2;
            int gap=8,bw=(pw-40-gap*2)/3,bh=31,by=y+174,bx=x+20;
            if(inside(mx,my,bx,by,bw,bh)){MusicManager.command("previous");return true;}
            if(inside(mx,my,bx+bw+gap,by,bw,bh)){MusicManager.command("toggle");return true;}
            if(inside(mx,my,bx+(bw+gap)*2,by,bw,bh)){MusicManager.command("next");return true;}
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==256||keyCode==GLFW_KEY_K){close();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    private static final int GLFW_KEY_K=75;
}
