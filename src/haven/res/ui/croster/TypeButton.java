/* Preprocessed source code */
package haven.res.ui.croster;

import haven.*;
import haven.render.*;
import java.util.*;
import java.util.function.*;
import haven.MenuGrid.Pagina;
import java.awt.Color;
import java.awt.image.BufferedImage;

@haven.FromResource(name = "ui/croster", version = 76)
public class TypeButton extends IButton {
    public final int order;

    public TypeButton(BufferedImage up, BufferedImage down, int order) {
	super(up, down);
	this.order = order;
    }

    protected void depress() {
	ui.sfx(Button.clbtdown.stream());
    }

    protected void unpress() {
	ui.sfx(Button.clbtup.stream());
    }
}
