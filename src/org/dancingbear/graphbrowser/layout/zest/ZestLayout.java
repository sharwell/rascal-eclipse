package org.dancingbear.graphbrowser.layout.zest;

import java.util.List;

import org.dancingbear.graphbrowser.layout.Layout;
import org.dancingbear.graphbrowser.layout.model.DirectedGraph;
import org.dancingbear.graphbrowser.layout.model.Edge;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.zest.layouts.InvalidLayoutConfiguration;
import org.eclipse.zest.layouts.LayoutEntity;
import org.eclipse.zest.layouts.LayoutRelationship;
import org.eclipse.zest.layouts.algorithms.AbstractLayoutAlgorithm;

/* 
 * This class delegates the layout to a given zest layout algorithm
 */
public class ZestLayout implements Layout {

	private AbstractLayoutAlgorithm algorithm;

	public ZestLayout(AbstractLayoutAlgorithm algorithm ) {
		this.algorithm = algorithm;
	}

	public void visit(DirectedGraph graph) {
		List entities = graph.getEntities();
		List relationships = graph.getRelationships();

		final LayoutEntity[] layoutEntities = new LayoutEntity[entities.size()];
		entities.toArray(layoutEntities);
		final LayoutRelationship[] layoutRelationships = new LayoutRelationship[relationships.size()];
		relationships.toArray(layoutRelationships);

		try {
			algorithm.applyLayout(layoutEntities, layoutRelationships, 0, 0, graph.getLayoutSize().height, graph.getLayoutSize().width, false, false);
		} catch (InvalidLayoutConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//it seems that the algorithm only updates the nodes but not the points of the edge
		//we need an extra phase to update the edges
		for (Edge e: graph.getEdges()) {
			PointList pts = new PointList();
			pts.addPoint(e.getSource().getX(), e.getSource().getY());
			pts.addPoint(e.getTarget().getX(),  e.getTarget().getY());
			e.setPoints(pts);
			// no spline defined for this edge so the translation will draw a straight line
			e.setSpline(null);
		}

	}

}