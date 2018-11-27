/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.run;

import com.vividsolutions.jts.geom.MultiPolygon;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.io.IOException;
import java.util.Map;

abstract public class RunMatsim {
	private static String fileConfig = "scenarios/zsd/config_spb_zsd_before.xml";
	private static String fileSHP = "input/inputForPlans/transport_districts_prim.shp";
	private static String SHPCoordSystem = "EPSG:4326"; //If WGS84 is used, then specify EPSG:4326
	private static String NetworkCoordSystem = "EPSG:32635";

	public static String getSHPCoordSystem() {
		return SHPCoordSystem;
	}

	public static String getNetworkCoordSystem() {
		return NetworkCoordSystem;
	}

	public static void main(String[] args) throws FactoryException, IOException {
		CoordinateReferenceSystem sourceCRS = CRS.decode(NetworkCoordSystem);
		CoordinateReferenceSystem targetCrs = CRS.decode(SHPCoordSystem);
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCrs);
		Map<Integer, MultiPolygon> MultiPolygonsMap = ParserSHP.run(fileSHP);
		Config config;
		if (args.length == 0 || args[0] == "") {
			config = ConfigUtils.loadConfig(fileConfig);
		} else {
			config = ConfigUtils.loadConfig(args[0]);
		}

		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		Handlers handlers = new Handlers(scenario, MultiPolygonsMap, transform);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handlers);
			}
		});

		controler.run();
	}


}
