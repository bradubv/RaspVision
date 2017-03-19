package org.team1635.vision;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

public class Quadrilateral {

	private MatOfPoint poly; // The polygon given to us.
	private Point vertices[]; // The quad that approximates that poly
	private Point topLeft;
	private Point topRight;
	private Point bottomRight;
	private Point bottomLeft;

	private Rect boundingRect;

	private boolean isDenaturated = false;

	public enum PolyDirection {
		TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT
	}

	/**
	 * After calling the constructor you must call either fromMatOfPoint or
	 * fromMatOfPoint2f
	 */
	public Quadrilateral() {
		vertices = new Point[4];
	}

	public void fromMatOfPoint2f(MatOfPoint2f quadCandidate2f) {
		MatOfPoint quadCandidate = new MatOfPoint();
		quadCandidate2f.convertTo(quadCandidate, CvType.CV_32S);
		fromMatOfPoint(quadCandidate);
	}

	public void fromMatOfPoint(MatOfPoint quadCandidate) {
		poly = healPoly(quadCandidate);

		if (!(isDenaturated)) {
			for (int vertCnt = 0; vertCnt < poly.rows(); vertCnt++) {
				Point point = new Point(poly.get(vertCnt, 0));
				vertices[vertCnt] = point;
			}
		}

		setBoundingRect();
	}
	
	private void setBoundingRect() {
		this.boundingRect = Imgproc.boundingRect(this.poly);
	}

	public int getHeight() {
		return boundingRect.height;
	}

	public int getWidth() {
		return boundingRect.width;
	}

	public boolean isDenaturated() {
		return isDenaturated;
	}

	public MatOfPoint healPoly(MatOfPoint polyIn) {
		List<Point> poly = new ArrayList<Point>();
		MatOfPoint polyOut = new MatOfPoint();

		List<Point> topLeft = new ArrayList<Point>();
		List<Point> topRight = new ArrayList<Point>();
		List<Point> bottomRight = new ArrayList<Point>();
		List<Point> bottomLeft = new ArrayList<Point>();

		Rect boundingRect = Imgproc.boundingRect(polyIn);
		Point center = new Point(boundingRect.x + boundingRect.width / 2, boundingRect.y + boundingRect.height / 2);

		for (int vertIdx = 0; vertIdx < polyIn.rows(); vertIdx++) {
			Point currVertex = new Point(polyIn.get(vertIdx, 0));

			if (currVertex.x > center.x) { // this point is on the right
				if (currVertex.y > center.y) { // this point is on the bottom
					bottomRight.add(currVertex);
				} else { // this point is on the top
					topRight.add(currVertex);
				}
			} else { // this point is on the left
				if (currVertex.y > center.y) { // this point is on bottom
					bottomLeft.add(currVertex);
				} else { // this point is on top
					topLeft.add(currVertex);
				}
			}
		}

		if (topLeft.size() == 1) {
			poly.add(topLeft.get(0));
			this.topLeft = topLeft.get(0);
		} else {
			if (topLeft.size() == 2) {
				Point hiddenPoint = findHiddenPoint(topLeft, PolyDirection.TOP_LEFT);
				poly.add(hiddenPoint);
				this.topLeft = hiddenPoint;
			} else {
				this.isDenaturated = true;
			}
		}
		if (topRight.size() == 1) {
			poly.add(topRight.get(0));
			this.topRight = topRight.get(0);
		} else {
			if (topRight.size() == 2) {
				Point hiddenPoint = findHiddenPoint(topRight, PolyDirection.TOP_RIGHT);
				poly.add(hiddenPoint);
				this.topRight = hiddenPoint;
			} else {
				this.isDenaturated = true;
			}
		}
		if (bottomRight.size() == 1) {
			poly.add(bottomRight.get(0));
			this.bottomRight = bottomRight.get(0);
		} else {
			if (bottomRight.size() == 2) {
				Point hiddenPoint = findHiddenPoint(bottomRight, PolyDirection.BOTTOM_RIGHT);
				poly.add(hiddenPoint);
				this.bottomRight = hiddenPoint;
			} else {
				this.isDenaturated = true;
			}
		}
		if (bottomLeft.size() == 1) {
			poly.add(bottomLeft.get(0));
			this.bottomLeft = bottomLeft.get(0);
		} else {
			if (bottomLeft.size() == 2) {
				Point hiddenPoint = findHiddenPoint(bottomLeft, PolyDirection.BOTTOM_LEFT);
				poly.add(hiddenPoint);
				this.bottomLeft = hiddenPoint;
			} else {
				this.isDenaturated = true;
			}
		}

		polyOut.fromList(poly);

		return polyOut;
	}

	/**
	 * 0 2 6 8 <= x 0 . . | . . | y of the smaller y, x of the larger y 2 . | .
	 * ------+----- 4 . | . | y of the bigger y, x of the smaller y 6 . . | . .
	 *
	 * ^ y
	 * 
	 * @param points
	 * @param dir
	 * @return
	 */
	private Point findHiddenPoint(List<Point> points, PolyDirection dir) {
		double x, y;

		// if you are in the top left corner or the top right corner you
		// want the x of the low point and the y of the high point.
		if ((dir == PolyDirection.TOP_LEFT) || (dir == PolyDirection.TOP_RIGHT)) {
			if (points.get(0).y > points.get(1).y) { // 0 has larger y
				x = points.get(0).x;
				y = points.get(1).y;
			} else { // 1 has larger y
				x = points.get(1).x;
				y = points.get(0).y;
			}
		} else {
			if (points.get(0).y > points.get(1).y) { // 0 has larger y
				x = points.get(1).x;
				y = points.get(0).y;
			} else { // 1 has larger y
				x = points.get(0).x;
				y = points.get(1).y;
			}
		}

		return new Point(x, y);
	}

	public void printPoly() {
		for (int vertCnt = 0; vertCnt < poly.rows(); vertCnt++) {
			Point point = new Point(poly.get(vertCnt, 0));
			System.out.println("Debug: Point : x = " + point.x + ", y = " + point.y);
		}
	}

	public void printOrientedCorners() {
		System.out.println("Debug: topLeft : x = " + getTopLeft().x + ", y = " + getTopLeft().y);
		System.out.println("Debug: topRight : x = " + getTopRight().x + ", y = " + getTopRight().y);
		System.out.println("Debug: bottomRight : x = " + getBottomRight().x + ", y = " + getBottomRight().y);
		System.out.println("Debug: bottomLeft : x = " + getBottomLeft().x + ", y = " + getBottomLeft().y);
	}

	public Point getTopLeft() {
		return topLeft;
	}
	
	public void setTopLeft(Point point) {
		this.topLeft = point;
		vertices[0] = point;
	}

	public Point getTopRight() {
		return topRight;
	}
	
	public void setTopRight(Point point) {
		this.topRight = point;
		vertices[1] = point;
	}

	public Point getBottomRight() {
		return bottomRight;
	}
	
	public void setBottomRight(Point point) {
		this.bottomRight = point;
		vertices[2] = point;
	}

	public Point getBottomLeft() {
		return bottomLeft;
	}
	
	public void setBottomLeft(Point point) {
		this.bottomLeft = point;
		vertices[3] = point;
	}

	public Point getCenter() {
		return new Point(boundingRect.x + boundingRect.width / 2, boundingRect.y + boundingRect.height / 2);
	}
	
	static public boolean lineUp(Quadrilateral q1, Quadrilateral q2) {
		if (Math.abs(q1.getCenter().x - q2.getCenter().x) < 5) {
			return true;
		} else {
			return false;
		}
	}
	
	static public Quadrilateral merge(Quadrilateral q1, Quadrilateral q2) {
		Quadrilateral newQuad = new Quadrilateral();
		if (q1.getCenter().y > q2.getCenter().y) {
			newQuad.setBottomLeft(q1.getBottomLeft());
			newQuad.setBottomRight(q1.getBottomRight());
			newQuad.setTopLeft(q2.getTopLeft());
			newQuad.setTopRight(q2.getTopRight());
		} else {
			newQuad.setBottomLeft(q2.getBottomLeft());
			newQuad.setBottomRight(q2.getBottomRight());
			newQuad.setTopLeft(q1.getTopLeft());
			newQuad.setTopRight(q1.getTopRight());			
		}
		newQuad.setPoly();
		return newQuad;
	}
	
	public void setPoly() {
		List<Point> polyArr = new ArrayList<Point>();
		polyArr.add(bottomLeft);
		polyArr.add(bottomRight);
		polyArr.add(topLeft);
		polyArr.add(topRight);
		poly.fromList(polyArr);
		setBoundingRect();
	}
}
