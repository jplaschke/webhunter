/*
 * To the extent possible under law, the ImageJ developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

package edu.umuc.student.jplaschke;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * Determines the bottom of a scanning electron microscope micrograph
 * Determine the scale length in pixels 
 * TODO: Determine the magnification and scale value, e.g. 10um
 * 
 * @author John Plaschke
 *  
 */
public class Read_Scale {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;
	private int semHeight =1;  // height minus metadata
    private int scaleWidth;
    
	// plugin parameters
	public double value;
	public String name;
	
	public Read_Scale() {
		semHeight = -1;
	}


	public int getSemHeight() {
		return semHeight;
	}

	public void setSemHeight(int semHeight) {
		this.semHeight = semHeight;
	}
	
	public int getScaleWidth() {
		return scaleWidth;
	}


	public void setScaleWidth(int scaleWidth) {
		this.scaleWidth = scaleWidth;
	}


	public ImagePlus getImage() {
		return image;
	}

	public void setImage(ImagePlus image) {
		this.image = image;
	}

    
	/**
	 * Process an image.
	 * <p>
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 * </p>
	 * <p>
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 * </p>
	 *
	 * @param image the image (possible multi-dimensional)
	 */
	public void process(ImagePlus image) {
		// slice numbers start with 1 for historical reasons
		IJ.log("image.getStackSize() "+image.getStackSize());
		for (int i = 1; i <= image.getStackSize(); i++)
			process(image.getStack().getProcessor(i));
	}

	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		int type = image.getType();
		width = ip.getWidth();
		height = ip.getHeight();
		if (type == ImagePlus.GRAY8) {
			byte[] pixels = process( (byte[]) ip.getPixels() );
			ip.setPixels(pixels);
		}
		else {
			throw new RuntimeException("not supported");
		}
	}

	// processing of GRAY8 images
	public byte[] process(byte[] pixels) {
		int blackTest = 0;
		int upperX = -1;
		int upperY = -1;
		int lowerX = -1;
		int lowerY = -1;
		
		//IJ.showMessage("height = "+height+" width = "+width);
		int longestRunWhite = 0; // longest run of white
		for (int y=0; y < height; y++) {
			blackTest = 0; // check for black line
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				// example: add 'number' to each pixel
				blackTest += (int)pixels[x + y * width];
			}
			if ((blackTest == 0) && (semHeight == -1)) {
				//IJ.showMessage("Found black line at y = "+y);
				semHeight = y;
				break;
			}
		}
		
		// Look for biggest white rectangle
		for (int y=semHeight; y < height; y++) {
			int currentX = -1;
			int curRunWhite = 0; // longest run of white
			for (int x=0; x < width; x++) {
				// Look for white rectangle
				if (pixels[x + y * width] == (byte)245) {
					if (currentX == -1) {
						currentX = x;
					}
					++curRunWhite;
				} else {
					if (curRunWhite > longestRunWhite) {
						longestRunWhite = curRunWhite;
						upperX = currentX;
						upperY = y;
					} else if (curRunWhite == longestRunWhite) {
						if (longestRunWhite > 280) {
							lowerX = x;
							lowerY = y;
						}
					}
					curRunWhite = -1;
					currentX = -1;
				}
			}
		
		}
		
		//IJ.showMessage("Final upperX = "+upperX+" upperY = "+upperY+" lowerX = "+lowerX+" lowerY = "+lowerY);
		for (int y=upperY; y < lowerY; y++) {
			for (int x=upperX; x < lowerX; x++) {
				pixels[x + y * width] -= (byte)60;		
			}
		}
		setScaleWidth(lowerX - upperX);
		return pixels;
	}

	public void showAbout() {
		IJ.showMessage("Read Scale",
			"reads the micrograph scale information"
		);
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads
	 * an image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Read_Scale.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
