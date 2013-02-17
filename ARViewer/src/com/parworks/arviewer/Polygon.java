package com.parworks.arviewer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Polygon {
	private String name;
	private String description;
	private String texture;
	private float[] color;
	private int numVertices;
	private float[] vertices;

	public Polygon(String name, String description, String texture, float[] color, float[] vertices) {
		this.name = name;
		this.description = description;
		this.texture = texture;
		this.color = new float[3];
		for (int i = 0; i < color.length; i++)
			this.color[i] = color[i];

		this.numVertices = vertices.length / 3;
		this.vertices = new float[this.numVertices * 7]; // x, y, z, r, g, b, a

		for (int i = 0; i < numVertices; i++) {
			for (int j = 0; j < 3; j++) {
				this.vertices[7 * i + j] = vertices[3 * i + j];
				//this.vertices[7 * i + j + 3] = color[j];
				this.vertices[7 * i + j + 3] = 0.5f;
			}
		}
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getTexture() {
		return texture;
	}

	public float[] getColor() {
		return color;
	}

	public int getNumVertices() {
		return numVertices;
	}

	public float[] getVertices() {
		return vertices;
	}

	public FloatBuffer getVertexFloatBuffer(float imageBitmapScaleFactor, float displayScaleFactor, float alpha) {
		float verticesInGLCoordinates[] = new float[vertices.length];
		for (int i = 0; i < numVertices; i++) {
			verticesInGLCoordinates[7 * i + 0] = vertices[7 * i + 0] * imageBitmapScaleFactor * displayScaleFactor;
			verticesInGLCoordinates[7 * i + 1] = vertices[7 * i + 1] * imageBitmapScaleFactor * displayScaleFactor;
			verticesInGLCoordinates[7 * i + 3] = vertices[7 * i + 3];
			verticesInGLCoordinates[7 * i + 4] = vertices[7 * i + 4];
			verticesInGLCoordinates[7 * i + 5] = vertices[7 * i + 5];
			verticesInGLCoordinates[7 * i + 6] = alpha;
		}

		ByteBuffer vbb = ByteBuffer.allocateDirect(verticesInGLCoordinates.length * 4); // float is 4 bytes
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer vfb = vbb.asFloatBuffer();
		vfb.put(verticesInGLCoordinates);
		vfb.position(0);

		return vfb;
	}
}
