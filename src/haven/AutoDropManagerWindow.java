package haven;

import haven.res.ui.pag.toggle.Toggle;

import java.util.Objects;

public class AutoDropManagerWindow extends Window {

    public static CheckBox autoDropItemsCheckBox;
    public static CheckBox includeOtherContainerInventoriesCheckBox;
    public static CheckBox onlyDropWhenPickaxeCursorIsActiveCheckBox;
    public static CheckBox autoDropStonesCheckbox;
    public static TextEntry autoDropStonesQualityTextEntry;
    public static CheckBox autoDropCoalsCheckbox;
    public static TextEntry autoDropCoalsQualityTextEntry;
    public static CheckBox autoDropOresCheckbox;
    public static TextEntry autoDropOresQualityTextEntry;
    public static CheckBox autoDropPreciousOresCheckbox;
    public static TextEntry autoDropPreciousOresQualityTextEntry;
    public static CheckBox autoDropMinedCuriosCheckbox;
    public static TextEntry autoDropMinedCuriosQualityTextEntry;
    public static CheckBox autoDropQuarryartzCheckbox;
    public static TextEntry autoDropQuarryartzQualityTextEntry;

    public AutoDropManagerWindow() {
        super(UI.scale(300, 350), "Auto-Drop Manager", true);

        Widget prev;
        prev = add(autoDropItemsCheckBox = new CheckBox("Enable Auto-Drop Items from Inventory") {
            {a = (Utils.getprefb("autoDropItems", false));}
            public void set(boolean val) {
                Utils.setprefb("autoDropItems", val);
                a = val;
                if (ui != null && ui.gui != null) {
                    ui.gui.optionInfoMsg("Auto-Drop Items from Inventory now " + (val ? "ENABLED" : "DISABLED") + "!", (val ? OptWnd.msgGreen : OptWnd.msgRed), Audio.resclip(val ? Toggle.sfxon : Toggle.sfxoff));
                }
            }
        }, 0, 6);
        prev = add(includeOtherContainerInventoriesCheckBox = new CheckBox("Include Other Container Inventories") {
            {a = (Utils.getprefb("includeOtherContainerInventories", false));}
            public void changed(boolean val) {
                Utils.setprefb("includeOtherContainerInventories", val);
            }
        }, prev.pos("bl").adds(0, 12));
        prev = add(onlyDropWhenPickaxeCursorIsActiveCheckBox = new CheckBox("Only Drop when Pickaxe Cursor is Active") {
            {a = (Utils.getprefb("onlyDropWhenPickaxeCursorIsActive", false));}
            public void changed(boolean val) {
                Utils.setprefb("onlyDropWhenPickaxeCursorIsActive", val);
            }
        }, prev.pos("bl").adds(0, 2).x(0));
        prev = add(new Label("Mining specific items:"), prev.pos("bl").adds(0, 12));
        prev = add(autoDropStonesCheckbox = new CheckBox("Stones"){
            {a = Utils.getprefb("autoDropStones", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropStones", val);
            }
        }, prev.pos("bl").adds(0, 10));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropStonesQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropStonesQuality", "30")){
            protected void changed() {
                Utils.setpref("autoDropStonesQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        prev = add(autoDropCoalsCheckbox = new CheckBox("Coals"){
            {a = Utils.getprefb("autoDropCoals", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropCoals", val);
            }
        }, prev.pos("bl").adds(0, 6));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropCoalsQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropCoalsQuality", "30")){
            protected void changed() {
                Utils.setpref("autoDropCoalsQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        prev = add(autoDropOresCheckbox = new CheckBox("Ores"){
            {a = Utils.getprefb("autoDropOres", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropOres", val);
            }
        }, prev.pos("bl").adds(0, 6));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropOresQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropOresQuality", "30")){
            protected void changed() {
                Utils.setpref("autoDropOresQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        prev = add(autoDropPreciousOresCheckbox = new CheckBox("Precious Ores"){
            {a = Utils.getprefb("autoDropPreciousOres", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropPreciousOres", val);
            }
        }, prev.pos("bl").adds(0, 6));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropPreciousOresQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropPreciousOresQuality", "999")){
            protected void changed() {
                Utils.setpref("autoDropPreciousOresQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        prev = add(autoDropMinedCuriosCheckbox = new CheckBox("Mined Curiosities"){
            {a = Utils.getprefb("autoDropMinedCurios", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropMinedCurios", val);
            }
        }, prev.pos("bl").adds(0, 6));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropMinedCuriosQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropMinedCuriosQuality", "30")){
            protected void changed() {
                Utils.setpref("autoDropMinedCuriosQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        prev = add(autoDropQuarryartzCheckbox = new CheckBox("Quarryartz"){
            {a = Utils.getprefb("autoDropQuarryartz", false);}
            public void changed(boolean val) {
                Utils.setprefb("autoDropQuarryartz", val);
            }
        }, prev.pos("bl").adds(0, 6));
        add(new Label("Q <"), prev.pos("ur").adds(0, 0).x(UI.scale(134)));
        add(autoDropQuarryartzQualityTextEntry = new TextEntry(UI.scale(36), Utils.getpref("autoDropQuarryartzQuality", "30")){
            protected void changed() {
                Utils.setpref("autoDropQuarryartzQuality", this.buf.line());
                this.settext(this.text().replaceAll("[^\\d]", ""));
                super.changed();
            }
        }, prev.pos("ur").adds(6, -2).x(UI.scale(156)));
        this.c = Utils.getprefc("wndc-autoDropManagerWindow", UI.unscale(new Coord(200, 100)));
        pack();
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if((sender == this) && (Objects.equals(msg, "close"))) {
            hide();
            Utils.setprefc("wndc-autoDropManagerWindow", this.c);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean show(boolean show) {
        Utils.setprefc("wndc-autoDropManagerWindow", this.c);
        return super.show(show);
    }

}
