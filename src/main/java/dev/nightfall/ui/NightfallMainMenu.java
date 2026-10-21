package dev.nightfall.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

public final class NightfallMainMenu extends Screen {
    public NightfallMainMenu() { super(Text.literal("NIGHTFALL")); }
    @Override public void render(DrawContext ctx,int mx,int my,float delta) {
        double t=System.currentTimeMillis()/1000.0; int w=width,h=height,cx=w/2;
        ctx.fillGradient(0,0,w,h,0xFF080910,0xFF15101F);
        for(int i=0;i<7;i++){int yy=(int)((h*(i/7.0)+(t*8+i*37)%h)%h);ctx.fill(0,yy,w,yy+1,0x1400D9FF);}
        ctx.fill(cx-150,0,cx-148,h,0x18B75CFF);ctx.fill(cx+148,0,cx+150,h,0x185CE1E6);
        ctx.fill(cx-92,h/2-113,cx+92,h/2-111,0xFFB75CFF);
        ctx.drawTextWithShadow(textRenderer,"N",cx-3,h/2-158,0xFFB75CFF);
        drawCentered(ctx,"NIGHTFALL",cx,h/2-136,0xFFF6F3FF);
        drawCentered(ctx,"CUSTOM CLIENT / MINECRAFT 1.21.4",cx,h/2-119,0xFF9297AD);
        int bw=224,bh=29,gap=7,bx=cx-bw/2,by=h/2-80;
        button(ctx,bx,by,bw,bh,"ОДИНОЧНАЯ ИГРА",mx,my,true);
        button(ctx,bx,by+bh+gap,bw,bh,"СЕТЕВАЯ ИГРА",mx,my,false);
        int sw=(bw-gap)/2,sy=by+(bh+gap)*2;
        button(ctx,bx,sy,sw,25,"НАСТРОЙКИ",mx,my,false);
        button(ctx,bx+sw+gap,sy,sw,25,"ВЫХОД",mx,my,false);
        drawCentered(ctx,"RIGHT SHIFT — CLICKGUI",cx,by+125,0xFF656C84);
        ctx.drawTextWithShadow(textRenderer,"NIGHTFALL // CLIENT VISUALS",12,h-18,0xFF555C72);
    }
    private void drawCentered(DrawContext ctx,String s,int cx,int y,int color){ctx.drawTextWithShadow(textRenderer,s,cx-textRenderer.getWidth(s)/2,y,color);}
    private void button(DrawContext ctx,int x,int y,int w,int h,String s,int mx,int my,boolean primary){
        boolean over=mx>=x&&mx<=x+w&&my>=y&&my<=y+h;
        int bg=primary?(over?0xFFF4DFFF:0xFFE9D4FA):(over?0xFF292139:0xD9181B29);
        ctx.fill(x,y,x+w,y+h,bg);ctx.fill(x,y,x+w,y+1,primary?0xFFFFFFFF:(over?0xFFB75CFF:0xFF303348));
        ctx.drawTextWithShadow(textRenderer,s,x+(w-textRenderer.getWidth(s))/2,y+(h-8)/2,primary?0xFF17111F:(over?0xFFF6EFFF:0xFFB0B5C9));
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0){
            int bw=224,bh=29,gap=7,cx=width/2,bx=cx-bw/2,by=height/2-80;
            if(inside(mx,my,bx,by,bw,bh)){client.setScreen(new SelectWorldScreen(this));return true;}
            if(inside(mx,my,bx,by+bh+gap,bw,bh)){client.setScreen(new MultiplayerScreen(this));return true;}
            int sw=(bw-gap)/2,sy=by+(bh+gap)*2;
            if(inside(mx,my,bx,sy,sw,25)){client.setScreen(new OptionsScreen(this,client.options));return true;}
            if(inside(mx,my,bx+sw+gap,sy,sw,25)){client.scheduleStop();return true;}
        }
        return super.mouseClicked(mx,my,button);
    }
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    @Override public boolean shouldCloseOnEsc(){return false;}
}
