package dev.nightfall.ui;

import dev.nightfall.Feature;
import dev.nightfall.FeatureRegistry;
import dev.nightfall.NightfallClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.List;

public final class ClickGuiScreen extends Screen {
    private Feature.Category selected = Feature.Category.RENDER;
    private int scroll;
    private final long openedAt = System.currentTimeMillis();
    public ClickGuiScreen() { super(Text.literal("NIGHTFALL ClickGUI")); }

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        int w = width, h = height;
        ctx.fillGradient(0, 0, w, h, 0xF9080A12, 0xF910101F);
        int pulse = (int)(25 + 20 * (0.5 + 0.5 * Math.sin((System.currentTimeMillis() - openedAt) / 500.0)));
        ctx.fill(0, 0, w, 2, (pulse << 24) | 0x00A64DFF);
        int pw = Math.min(650, w - 42), ph = Math.min(390, h - 34), x = (w-pw)/2, y=(h-ph)/2;
        ctx.fill(x+4,y+5,x+pw+4,y+ph+5,0x70000000); ctx.fill(x,y,x+pw,y+ph,0xF20D101B);
        ctx.fill(x,y,x+pw,y+1,0xFF27283C); ctx.fill(x,y,x+2,y+ph,0xFFB45CFF);
        int sw=142; ctx.fill(x+2,y+1,x+sw,y+ph-1,0xA80A0C15);
        ctx.drawTextWithShadow(textRenderer,"NIGHTFALL",x+16,y+17,0xFFF4F4FF);
        ctx.drawTextWithShadow(textRenderer,"CLIENT / 1.21.4",x+16,y+31,0xFF8C90A8);
        ctx.fill(x+14,y+49,x+sw-14,y+50,0xFF292B40);
        Feature.Category[] cats=Feature.Category.values();
        for(int i=0;i<cats.length;i++){
            int cy=y+61+i*31; boolean active=cats[i]==selected;
            if(active){ctx.fill(x+10,cy-5,x+sw-10,cy+20,0xD328203F);ctx.fill(x+10,cy-5,x+12,cy+20,0xFFB75CFF);}
            ctx.drawTextWithShadow(textRenderer,cats[i].title(),x+20,cy+2,active?0xFFF4E8FF:0xFF9B9FB2);
            String n=Integer.toString(FeatureRegistry.in(cats[i]).size());
            ctx.drawTextWithShadow(textRenderer,n,x+sw-27-textRenderer.getWidth(n),cy+2,0xFF626981);
        }
        int tx=x+sw+17,cw=pw-sw-31;
        ctx.drawTextWithShadow(textRenderer,selected.title().toUpperCase(),tx,y+17,0xFFB75CFF);
        ctx.drawTextWithShadow(textRenderer,"Нажми на карточку, чтобы переключить функцию",tx,y+31,0xFF8F95AA);
        ctx.fill(tx,y+48,x+pw-17,y+49,0xFF292B40);
        List<Feature> list=FeatureRegistry.in(selected);
        int cardW=(cw-10)/2, cardH=57, startY=y+60, visible=Math.max(1,(ph-80)/(cardH+8));
        int maxScroll=Math.max(0,(int)Math.ceil(list.size()/2.0)-visible); scroll=Math.max(0,Math.min(scroll,maxScroll));
        for(int i=0;i<list.size();i++){
            int col=i%2,row=i/2-scroll; if(row<0||row>=visible)continue;
            int bx=tx+col*(cardW+10),by=startY+row*(cardH+8); Feature f=list.get(i);
            boolean over=mx>=bx&&mx<=bx+cardW&&my>=by&&my<=by+cardH;
            ctx.fill(bx,by,bx+cardW,by+cardH,f.enabled()?0xE51A1728:(over?0xD7181B29:0xB9121420));
            ctx.fill(bx,by,bx+cardW,by+1,f.enabled()?0xFFB75CFF:0xFF272B3B);
            if(f.enabled())ctx.fill(bx,by+1,bx+2,by+cardH,0xFFB75CFF);
            ctx.drawTextWithShadow(textRenderer,f.name(),bx+10,by+9,f.enabled()?0xFFF8EFFF:0xFFE0E3F0);
            String status=f.enabled()?"ON":"OFF";
            ctx.drawTextWithShadow(textRenderer,status,bx+cardW-textRenderer.getWidth(status)-9,by+10,f.enabled()?0xFF61E6D1:0xFF777D94);
            ctx.drawTextWithShadow(textRenderer,f.description(),bx+10,by+29,0xFF8D93A9);
        }
        String footer="RIGHT SHIFT  ·  закрыть    |    настройки сохраняются";
        ctx.drawTextWithShadow(textRenderer,footer,x+16,y+ph-17,0xFF646B84);
        if(NightfallClient.enabled("menuinfo")){
            String mode=MinecraftClient.getInstance().isInSingleplayer()?"SINGLEPLAYER":"CLIENT";
            ctx.drawTextWithShadow(textRenderer,mode,x+pw-textRenderer.getWidth(mode)-17,y+ph-17,0xFF61E6D1);
        }
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return super.mouseClicked(mx,my,button);
        int pw=Math.min(650,width-42),ph=Math.min(390,height-34),x=(width-pw)/2,y=(height-ph)/2,sw=142;
        for(int i=0;i<Feature.Category.values().length;i++){
            int cy=y+61+i*31;
            if(mx>=x+8&&mx<=x+sw-8&&my>=cy-7&&my<=cy+23){selected=Feature.Category.values()[i];scroll=0;return true;}
        }
        int tx=x+sw+17,cw=pw-sw-31,cardW=(cw-10)/2,cardH=57,startY=y+60,visible=Math.max(1,(ph-80)/(cardH+8));
        List<Feature> list=FeatureRegistry.in(selected);
        for(int i=0;i<list.size();i++){
            int col=i%2,row=i/2-scroll;if(row<0||row>=visible)continue;
            int bx=tx+col*(cardW+10),by=startY+row*(cardH+8);
            if(mx>=bx&&mx<=bx+cardW&&my>=by&&my<=by+cardH){Feature f=list.get(i);NightfallClient.setFeatureEnabled(f.id(),!f.enabled());return true;}
        }
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){scroll=Math.max(0,scroll-(int)Math.signum(vertical));return true;}
    @Override public boolean shouldCloseOnEsc(){return true;}
}
