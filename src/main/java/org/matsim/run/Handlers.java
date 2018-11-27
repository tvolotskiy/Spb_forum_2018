package org.matsim.run;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PopulationUtils;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import java.util.HashSet;
import java.util.Map;

public class Handlers implements PersonDepartureEventHandler {
    private Network network;
    private Scenario scenario;
    private MathTransform transform;
    private Map<Integer, MultiPolygon> MultiPolygonsMap;
    private HashSet<Id<Person>> setOfAgentFromSHPZone = new HashSet<>();
    private static GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();

    Handlers(Scenario scenario, Map<Integer, MultiPolygon> MultiPolygonsMap, MathTransform transform) {
        this.scenario = scenario;
        this.network = scenario.getNetwork();
        this.transform = transform;
        this.MultiPolygonsMap = MultiPolygonsMap;
    }

    private boolean checkZone(Coord coord) {
        Point point;
        Point inverse_point = null;
        //If SHP Coordinate System equals Network Coordinate System then just create point WITHOUT transformation
        if (RunMatsim.getSHPCoordSystem().equals(RunMatsim.getNetworkCoordSystem())) {
            point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
            //Else create point WITH transformation Network Coodinate System to SHP Coordinate System
        } else {
            point = geometryFactory.createPoint(new Coordinate(coord.getX(), coord.getY()));
            try {
                point = (Point) JTS.transform(point, transform);
                inverse_point = geometryFactory.createPoint(new Coordinate(point.getY(), point.getX()));
            } catch (TransformException e) {
                e.printStackTrace();
            }
        }

        for (Map.Entry<Integer, MultiPolygon> entry : MultiPolygonsMap.entrySet()) {
            if (entry.getValue().contains(inverse_point)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        boolean resultOfChecking = checkZone(network.getLinks().get(event.getLinkId()).getCoord());
        Id<Person> personId = event.getPersonId();
        if (resultOfChecking) {
            Person person = scenario.getPopulation().getPersons().get(personId);
            String firstActivityType = PopulationUtils.getActivities(person.getSelectedPlan(), null).get(0).getType();
            String secondActivityType = PopulationUtils.getActivities(person.getSelectedPlan(), null).get(1).getType();
            if(firstActivityType.equals("h")&&secondActivityType.equals("w")) {
                setOfAgentFromSHPZone.add(personId);
            }
        }
    }
}
