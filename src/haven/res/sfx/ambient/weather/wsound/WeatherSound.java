/* Preprocessed source code */
package haven.res.sfx.ambient.weather.wsound;

import haven.*;
import haven.render.*;

/* >wtr: WeatherSound */
@haven.FromResource(name = "sfx/ambient/weather/wsound", version = 1)
public class WeatherSound implements Glob.Weather, RenderTree.Node {
    /* XXX: The location should also be nil, but ActAudio.PosClip
     * actually seem to be buggy in how it determines the balance of a
     * sounds. Please investigate and fix. */
    public static final Pipe.Op nopos = Pipe.Op.compose(Homo3D.cam.nil, Location.xlate(Coord3f.of(0, 0, -10)));
    public final RenderTree.Node spr;
    public final Volume vol;
    public static boolean volumeUpdated = false;

    public interface Volume {
	public void setvol(double vol);
    }

    public WeatherSound(Resource res, Object[] args) {
	ClipAmbiance.Desc amb = res.layer(ClipAmbiance.Desc.class);
	if(amb != null) {
	    ClipAmbiance spr = new ClipAmbiance(amb);
	    this.spr = spr;
	    this.vol = vol -> {spr.bvol = vol;};
	} else {
	    this.spr = Sprite.create(null, res, Message.nil);
	    if(this.spr instanceof Volume)
		this.vol = (Volume)this.spr;
	    else
		this.vol = vol -> {};
	}
	update(args);
    }

    public Pipe.Op state() {return(null);}

    public void update(Object[] args) {
	double vol = 1.0;
	if(args.length > 0)
	    vol = ((Number)args[0]).doubleValue() * 0.01;
	this.vol.setvol(vol * OptWnd.weatherSoundVolumeSlider.val/100d);
    }

    public boolean tick(double dt) {
    if (volumeUpdated){
        this.vol.setvol(OptWnd.weatherSoundVolumeSlider.val/100d);
        volumeUpdated = false;
    }
	return(false);
    }

    public void added(RenderTree.Slot slot) {
	slot.ostate(nopos);
	slot.add(spr);
    }
}
