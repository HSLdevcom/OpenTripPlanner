package org.opentripplanner.annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import external.service.ExternalGraphUpdater;
import java.io.File;
import java.util.Collections;
import org.junit.Test;
import org.opentripplanner.annotation.StaticClassComponent.FinalStaticClassComponent;
import org.opentripplanner.routing.impl.NycFareServiceFactory;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.updater.GraphUpdater;

public class ComponentAnnotationConfiguratorTest {

  ComponentAnnotationConfigurator configurator = ComponentAnnotationConfigurator.getInstance();

  @Test
  public void testNormalGraphUpdaterComponent()
      throws InstantiationException, IllegalAccessException {
    configurator.scanPackages(Collections.singletonList("org.opentripplanner.annotation"));
    GraphUpdater component = configurator
        .getComponentInstance("test.compoent", ServiceType.GraphUpdater);
    assertNotNull(component);
    assertTrue(TestComponent.class.isAssignableFrom(component.getClass()));
  }

  @Test
  public void testStaticInnerClass() throws InstantiationException, IllegalAccessException {
    configurator.scanPackages(Collections.singletonList("org.opentripplanner.annotation"));
    GraphUpdater component = configurator
        .getComponentInstance("test.staticClassComponent", ServiceType.GraphUpdater);
    assertNotNull(component);
    assertTrue(StaticClassComponent.class.isAssignableFrom(component.getClass()));
    assertTrue(FinalStaticClassComponent.class.isAssignableFrom(component.getClass()));
  }

  @Test
  public void testFromBuildConfig() throws InstantiationException, IllegalAccessException {
    JsonNode builderConfig = OTPMain
        .loadJson(new File("./src/test/resources/", "build-config.json"));
    ComponentAnnotationConfigurator.getInstance().fromConfig(builderConfig);
    GraphUpdater component = configurator
        .getComponentInstance("external.updater", ServiceType.GraphUpdater);
    assertNotNull(component);
    assertTrue(GraphUpdater.class.isAssignableFrom(component.getClass()));
    assertTrue(ExternalGraphUpdater.class.isAssignableFrom(component.getClass()));

    FareServiceFactory factory = configurator
        .getComponentInstance("new-york", ServiceType.ServiceFactory);
    assertNotNull(factory);
    assertTrue(NycFareServiceFactory.class.isAssignableFrom(factory.getClass()));
    assertTrue(FareServiceFactory.class.isAssignableFrom(factory.getClass()));
  }
}
