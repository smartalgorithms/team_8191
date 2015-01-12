/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package team_152;

/**
 *
 * @author byrdie
 */
public class flockBug {

    /**
     * -1 means not tracing, 0 means tracing wall on left, 1 means on right.
     */
    int tracing = -1;
    /**
     * direction of the wall we're currently hugging.
     */
    int traceDirection = -1;
    /**
     * x-coord of point we started tracing at.
     */
    int xTraceStart = -1;
    /**
     * y-coord of point we started tracing at.
     */
    int yTraceStart = -1;
    /**
     * x-coord of the destination as of last turn.
     */
    int xPrevDest = -1;
    /**
     * y-coord of the destination as of last turn.
     */
    int yPrevDest = -1;
    /**
     * number of turns that the bug has been tracing for.
     */
    int turnsTraced = 0;
    /**
     * distance to destination we started tracing at. Leave trace mode when
     * current distance to destination is below this value.
     */
    double traceDistance = -1;
    /**
     * the default direction to trace. Changes every time we trace too far.
     */
    int defaultTraceDirection = 0;
    /**
     * trace threshold to reset to every time we get a new destination.
     */
    final int INITIAL_TRACE_THRESHOLD = 5;
    /**
     * number of turns to trace before resetting.
     */
    int traceThreshold = -1;
}
