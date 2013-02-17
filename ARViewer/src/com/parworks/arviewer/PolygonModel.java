package com.parworks.arviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import android.graphics.Point;
import android.util.Log;

import com.parworks.androidlibrary.ar.AugmentedData;
import com.parworks.androidlibrary.ar.Overlay;
import com.parworks.androidlibrary.response.ImageOverlayInfo;
import com.parworks.androidlibrary.response.OverlayPoint;

public class PolygonModel {
	
	private static final String TAG = PolygonModel.class.getName();
	
	private List<Polygon> polygons = new ArrayList<Polygon>();
	private boolean localized;
	private float focalLength;
	private float score;
	private float[] fov = new float[2];

	public List<Polygon> getPolygons() {
		return polygons;
	}

	public int getNumPolygons() {
		return polygons.size();
	}

	public void setLocalized(boolean success) {
		localized = success;
	}

	public boolean getLocalized() {
		return localized;
	}

	public void setFocalLength(float f) {
		focalLength = f;
	}

	public float getFocalLength() {
		return focalLength;
	}

	public void setScore(float s) {
		score = s;
	}

	public float getScore() {
		return score;
	}

	public void setFieldOfView(float[] fovd) {
		fov = fovd;
	}

	public float[] getFieldOfView() {
		return fov;
	}

	public int getClickedPolygonIndex(float Px, float Py) {
		float Ax, Ay, Bx, By, Cx, Cy;
		float v0x, v0y, v1x, v1y, v2x, v2y;
		float dot00, dot01, dot02, dot11, dot12, invDenom;
		float u, v;

		for (int i = 0; i < polygons.size(); i++) {
			float[] vertices = polygons.get(i).getVertices();
			Ax = vertices[0];
			Ay = vertices[1];
			
			for (int j = 7; j < vertices.length - 7; j += 7) {
				Bx = vertices[j];
				By = vertices[j + 1];
				Cx = vertices[j + 7];
				Cy = vertices[j + 8];
				v0x = Cx - Ax;
				v0y = Cy - Ay;
				v1x = Bx - Ax;
				v1y = By - Ay;
				v2x = Px - Ax;
				v2y = Py - Ay;

				dot00 = v0x * v0x + v0y * v0y;
				dot01 = v0x * v1x + v0y * v1y;
				dot02 = v0x * v2x + v0y * v2y;
				dot11 = v1x * v1x + v1y * v1y;
				dot12 = v1x * v2x + v1y * v2y;

				// Compute barycentric coordinates
				invDenom = 1f / (dot00 * dot11 - dot01 * dot01);
				u = (dot11 * dot02 - dot01 * dot12) * invDenom;
				v = (dot00 * dot12 - dot01 * dot02) * invDenom;

				if (u >= 0 && v >= 0 && u + v < 1f)
					return i;
			}
		}

		return -1;
	}

	public String getPolygonName(int location) {
		if (location >= polygons.size())
			return "";
		else
			return polygons.get(location).getName();
	}

	public String getPolygonDescription(int location) {
		if (location >= polygons.size())
			return "";
		else
			return polygons.get(location).getDescription();
	}
	
	public List<ImageOverlayInfo> getImageOverlays(){
		List<ImageOverlayInfo> overlays = new ArrayList<ImageOverlayInfo>(polygons.size());
		for(Polygon p : polygons){
			ImageOverlayInfo info = new ImageOverlayInfo();
			info.setContent(p.getDescription());
			info.setName(p.getName());
			
			float[] vertices = p.getVertices();
			
			List<OverlayPoint> points = new ArrayList<OverlayPoint>();
			for(int i = 0; i < (vertices.length / 7); i++) {
				OverlayPoint point = new OverlayPoint();
				point.setX(vertices[i * 7]);
				point.setY(vertices[i * 7 + 1]);
				points.add(point);
			}
			
			info.setPoints(points);
			
			overlays.add(info);
		}
		
		return overlays;
	}
	
	public static PolygonModel readImageOverlayList(List<ImageOverlayInfo> overlayList) {
		PolygonModel pm = new PolygonModel();
		for(ImageOverlayInfo overlay : overlayList) {			
			// convert from Vertex to float[]
			float[] vertices = new float[overlay.getPoints().size() * 3];
			for(int i = 0; i < overlay.getPoints().size(); i++) {
				vertices[i * 3] = overlay.getPoints().get(i).getX();
				vertices[i * 3 + 1] = overlay.getPoints().get(i).getY();
				vertices[i * 3 + 2] = 1.0f;
			}			
			Polygon p = new Polygon(overlay.getName(),
					overlay.getContent(), null, new float[0], vertices);
			pm.polygons.add(p);
		}
		return pm;
	}
	
	public static PolygonModel readAugmentedData(AugmentedData augmentedData, Point imageSize, Point originalSize) {
		PolygonModel pm = new PolygonModel();
		if (augmentedData.isLocalization()) {		
			if (augmentedData.getScore() != null) {
				pm.setScore(Float.parseFloat(augmentedData.getScore()));
			}
			if (augmentedData.getFocalLength() != null) {
				pm.setFocalLength(Float.parseFloat(augmentedData.getFocalLength()));	
			}			
			pm.setLocalized(augmentedData.isLocalization());
			String[] fov = augmentedData.getFov().split(",");
			pm.setFieldOfView(new float[]{Float.parseFloat(fov[0]), Float.parseFloat(fov[1])});
			
			// scale factor
			float xscale = ((float) imageSize.x) / ((float) originalSize.x);
			float yscale = ((float) imageSize.y) / ((float) originalSize.y);
			float scale = Math.min(xscale, yscale);		
			
			for(Overlay overlay : augmentedData.getOverlays()) {
				// convert from Vertex to float[]
				float[] vertices = new float[overlay.getVertices().size() * 3];
				for(int i = 0; i < overlay.getVertices().size(); i++) {
					vertices[i * 3] = overlay.getVertices().get(i).getxCoord() * scale;
					vertices[i * 3 + 1] = overlay.getVertices().get(i).getyCoord() * scale;
					vertices[i * 3 + 2] = overlay.getVertices().get(i).getzCoord();
				}
				Polygon p = new Polygon(overlay.getName(),
						overlay.getDescription(), null, new float[0], vertices);
				pm.polygons.add(p);
			}
		} else {
			Log.w(TAG, "The given augmentedData is not localized.");
		}
		return pm;
	}

	public static PolygonModel readFile(File f) {
		PolygonModel pm = new PolygonModel();
		BufferedReader br = null;

		try {
			FileReader fr = new FileReader(f);
			br = new BufferedReader(fr);
			String line, key, name = "", description = "", texture = "";
			float[] color = new float[3];
			float[] vertices = null;

			while ((line = br.readLine()) != null) {
				StringTokenizer entry = new StringTokenizer(line, "=");
				if (entry.countTokens() != 2)
					continue;

				key = entry.nextToken();

				if (key.compareTo("localization") == 0)
					pm.setLocalized(Boolean.parseBoolean(entry.nextToken()));

				else if (key.compareTo("focallength") == 0)
					pm.setFocalLength(Float.parseFloat(entry.nextToken()));

				else if (key.compareTo("score") == 0)
					pm.setScore(Float.parseFloat(entry.nextToken()));

				else if (key.compareTo("fov") == 0) {
					StringTokenizer values = new StringTokenizer(entry.nextToken(), ",");
					if (values.countTokens() != 2)
						continue;

					float[] fovd = new float[2];
					fovd[0] = Float.parseFloat(values.nextToken());
					fovd[1] = Float.parseFloat(values.nextToken());

					pm.setFieldOfView(fovd);
				}

				else if (key.compareTo("name") == 0)
					name = entry.nextToken();

				else if (key.compareTo("description") == 0)
					description = entry.nextToken();

				else if (key.compareTo("texture") == 0)
					texture = entry.nextToken();

				else if (key.compareTo("color") == 0) {
					StringTokenizer values = new StringTokenizer(entry.nextToken(), ",");
					if (values.countTokens() != 3)
						continue;

					color[0] = Float.parseFloat(values.nextToken());
					color[1] = Float.parseFloat(values.nextToken());
					color[2] = Float.parseFloat(values.nextToken());

					if (color[0] > 1f || color[1] > 1f || color[2] > 1f) {
						color[0] /= 255;
						color[1] /= 255;
						color[2] /= 255;
					}

				} else if (key.compareTo("vertices") == 0) { // one polygon info must be ended with vertices entry
					StringTokenizer values = new StringTokenizer(entry.nextToken(), ",");
					if (values.countTokens() % 3 != 0)
						continue;

					int length = values.countTokens();
					vertices = new float[length];
					for (int i = 0; i < length; i++)
						vertices[i] = Float.parseFloat(values.nextToken());
					pm.polygons.add(new Polygon(name, description, texture, color, vertices));
					vertices = null;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NumberFormatException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return pm;
	}
}
