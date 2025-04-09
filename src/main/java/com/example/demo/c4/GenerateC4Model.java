package com.example.demo.c4;

import com.structurizr.Workspace;
import com.structurizr.model.*;
import com.structurizr.view.*;
import org.reflections.Reflections;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;

import com.structurizr.io.json.JsonWriter;

public class GenerateC4Model {

    public static void main(String[] args) throws Exception {

        Workspace workspace = new Workspace("C4 Model", "Automatically generated C4 model with relationships");
        Model model = workspace.getModel();
        ViewSet views = workspace.getViews();

        // add user for the model
        Person user = model.addPerson("User", "Uses the application");
        //add SoftwareSystem for the model
        SoftwareSystem system = model.addSoftwareSystem("Spring Boot App", "Generated automatically");
        // add container called web application
        Container webApp = system.addContainer("Web Application", "Spring Boot App", "Java");
        // add relation between user and web application
        user.uses(webApp, "Uses");
        //introspect upon the java program
        Reflections reflections = new Reflections("com.example.demo");
        //hashmap to save the component
        Map<Class<?>, Component> componentMap = new HashMap<>();

        // Scan for components in every class
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Controller.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Spring Controller", "Java");
            componentMap.put(clazz, c);
            user.uses(c, "Sends HTTP requests");
        }
        // scan for services in every class
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Service.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Spring Service", "Java");
            componentMap.put(clazz, c);
        }
        // scan for repositories in every class
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Repository.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Spring Repository", "Java");
            componentMap.put(clazz, c);
        }

        // Automatically detect relationships based on field types
        for (Map.Entry<Class<?>, Component> entry : componentMap.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Component sourceComponent = entry.getValue();

            for (Field field : sourceClass.getDeclaredFields()) {
                Class<?> targetType = field.getType();
                Component targetComponent = componentMap.get(targetType);
                if (targetComponent != null) {
                    sourceComponent.uses(targetComponent, "Uses via field: " + field.getName());
                }
            }
        }

        // Views
        ContainerView containerView = views.createContainerView(system, "containers", "Container view");
        containerView.addAllElements();

        ComponentView componentView = views.createComponentView(webApp, "components", "Component view");
        componentView.addAllComponents();
        componentView.add(user);

        // Styles
        Styles styles = views.getConfiguration().getStyles();
        styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);
        styles.addElementStyle("Component").background("#85bb65").color("#ffffff").shape(Shape.Hexagon);
        styles.addElementStyle("Container").background("#438dd5").color("#ffffff");

        // Export as workspace.json for Structurizr Lite
        try (Writer writer = new FileWriter("workspace.json")) {
            new JsonWriter(true).write(workspace, writer);
            System.out.println("✅ C4 model generated to workspace.json");
        }
    }
}
