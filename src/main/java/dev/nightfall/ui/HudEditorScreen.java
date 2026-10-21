package dev.nightfall.ui;

import dev.nightfall.HudLayout;
import dev.nightfall.NightfallClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;

public final class HudEditorScreen extends Screen {
    private record Widget(String id, String label, String module, int defaultX, int defaultY, int width, int height) {}
    private final List<Widget> widgets;
    private Widget dragging;
    private int dragDx, dragDy;

    public HudEditorScreen() {
        super(Text.literal("NIGHTFALL HUD Editor"));
        int w = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledWidth();
        int h = net.minecraft.client.MinecraftClient.getInstance().getWindow().getScaledHeight();
        widgets = List.of(
            new Widget("watermark","NIGHTFALL","watermark",9,9,116,25),
            new Widget("fps","FPS  240","fps",w-82,9,73,18),
            new Widget("coordinates","X 120  Y 64  Z -20","coordinates",9,h-28,145,18),
            new Widget("keystrokes","W  A  S  D","keystrokes",w-145,h-29,136,18),
            new Widget("mediaplayer","MEDIA PLAYER","mediaplayer",w-260,39,248,45),
            new Widget("worldlyrics","WORLD LYRICS","worldlyrics",w/2-175,h-57,350,25),
            new Widget("radar","PLAYER RADAR","radar",w-108,38,90,90),
            new Widget("targethud","TARGET HUD","targethud",w/2-80,h/2+35,160,35),
            new Widget("speedometer","SPEEDOMETER","speedometer",12,h-48,110,18),
            new Widget("totemcount","TOTEM COUNT","totemcount",12,37,110,18),
            new Widget("armorhud","ARMOR HUD","armorhud",w/2-70,h-61,140,18),
            new Widget("potionhud","EFFECTS HUD","potionhud",w-120,55,110,18)
        );
        HudLayout.load();
    }

    @Override public void render(DrawContext ctx,int mx,int my,float delta) {
        ctx.fill(0,0,width,height,0x85060B0E);
        ctx.fill(0,0,width,2,0xFF35434B);
        text(ctx,"NIGHTFALL  /  HUD EDITOR",14,12,0xFF79BDB5);
        text(ctx,"Перетаскивай блоки мышью • ПКМ включает/выключает модуль • ESC закрыть",14,28,0xFF9BAAAF);
        for (Widget widget : widgets) {
            int[] p=HudLayout.get(widget.id(),widget.defaultX(),widget.defaultY());
            int x=Math.max(0,Math.min(width-widget.width(),p[0]));
            int y=Math.max(36,Math.min(height-widget.height(),p[1]));
            boolean hover=mx>=x&&mx<=x+widget.width()&&my>=y&&my<=y+widget.height();
            boolean enabled=NightfallClient.enabled(widget.module());
            int fill=enabled?0xB923353A:0xAE1A2329;
            ctx.fill(x,y,x+widget.width(),y+widget.height(),fill);
            ctx.fill(x,y,x+widget.width(),y+1,enabled?0xFF79BDB5:0xFF35434B);
            ctx.fill(x,y,x+2,y+widget.height(),hover?0xFFB5DCD5:0xFF52656B);
            text(ctx,widget.label(),x+7,y+6,enabled?0xFFE2EFED:0xFF89989D);
        }
        text(ctx,"HUD позиции сохраняются в config/nightfall/hud-layout.json",14,height-18,0xFF60737B);
    }
    private void text(DrawContext ctx,String s,int x,int y,int color){
        ctx.drawTextWithShadow(textRenderer,UiText.of(s),x,y,color);
    }

    @Override public boolean mouseClicked(double mx,double my,int button) {
        for(Widget widget:widgets){
            int[] p=HudLayout.get(widget.id(),widget.defaultX(),widget.defaultY());
            int x=Math.max(0,Math.min(width-widget.width(),p[0]));
            int y=Math.max(36,Math.min(height-widget.height(),p[1]));
            if(mx>=x&&mx<=x+widget.width()&&my>=y&&my<=y+widget.height()){
                if(button==1){
                    NightfallClient.setFeatureEnabled(widget.module(),!NightfallClient.enabled(widget.module()));
                    return true;
                }
                if(button==0){
                    dragging=widget;dragDx=(int)mx-x;dragDy=(int)my-y;return true;
                }
            }
        }
        return true;
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy) {
        if(dragging!=null&&button==0){
            HudLayout.set(dragging.id(),(int)mx-dragDx,(int)my-dragDy);
            return true;
        }
        return super.mouseDragged(mx,my,button,dx,dy);
    }
    @Override public boolean mouseReleased(double mx,double my,int button) {
        if(dragging!=null){
            dragging=null;HudLayout.save();return true;
        }
        return super.mouseReleased(mx,my,button);
    }
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers) {
        if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_CONTROL){close();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override public boolean shouldCloseOnEsc(){return true;}
}
