package com.parworks.arviewer.utils;

import java.util.ArrayList;
import java.util.List;

import com.parworks.androidlibrary.ar.AugmentedData;
import com.parworks.androidlibrary.ar.Overlay;
import com.parworks.androidlibrary.ar.OverlayImpl;
import com.parworks.androidlibrary.ar.Vertex;
import com.parworks.androidlibrary.response.AugmentImageResultResponse;
import com.parworks.androidlibrary.response.OverlayAugmentResponse;

public class AugmentedDataUtils {

	public static AugmentedData convertAugmentResultResponse(String imgId,
			AugmentImageResultResponse result) {
		List<OverlayAugmentResponse> overlayResponses = result.getOverlays();
		List<Overlay> overlays = new ArrayList<Overlay>();

		for (OverlayAugmentResponse overlayResponse : overlayResponses) {
			overlays.add(makeOverlay(overlayResponse, imgId));
		}

		AugmentedData augmentedData = new AugmentedData(result.getFov(),
				result.getFocalLength(), result.getScore(),
				result.isLocalization(), overlays);
		return augmentedData;
	}
	
	private static Overlay makeOverlay(OverlayAugmentResponse overlayResponse,
			String imgId) {
		Overlay overlay = new OverlayImpl(imgId, overlayResponse.getName(),
				overlayResponse.getDescription(),
				parseVertices(overlayResponse.getVertices()));
		return overlay;
	}

	private static List<Vertex> parseVertices(String serverOutput) {
		String[] points = serverOutput.split(",");

		List<Vertex> vertices = new ArrayList<Vertex>();
		for (int i = 0; i < points.length; i += 3) {
			float xCoord = Float.parseFloat(points[i]);
			float yCoord = Float.parseFloat(points[i + 1]);
			float zCoord = Float.parseFloat(points[i + 2]);
			vertices.add(new Vertex(xCoord, yCoord, zCoord));
		}
		return vertices;
	}
}
