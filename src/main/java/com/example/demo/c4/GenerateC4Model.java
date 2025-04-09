package com.example.demo.c4;

import com.structurizr.Workspace;
import com.structurizr.model.*;
import com.structurizr.view.*;
import com.structurizr.io.json.JsonWriter;
import org.reflections.Reflections;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

// https://www.baeldung.com/structurizr

public class GenerateC4Model {

    public static void main(String[] args) throws Exception {

        // Load application.properties
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
            props.load(input);
        }

        // Get database properties
        String jdbcUrl = props.getProperty("spring.datasource.url");
        String username = props.getProperty("spring.datasource.username");
        String password = props.getProperty("spring.datasource.password");

        // Create a workspace for the C4 model
        Workspace workspace = new Workspace("C4 Model", "Spring Boot + MySQL");

        // Create the model section
        Model model = workspace.getModel();

        // Create the view section
        ViewSet views = workspace.getViews();

        // Add a person
        Person user = model.addPerson("User", "Uses the application");

        // Create a SoftwareSystem for the Spring Boot application
        SoftwareSystem system = model.addSoftwareSystem("Spring Boot App", "Generated C4 model");

        // Create a container called webApp
        Container webApp = system.addContainer("Web Application", "Spring Boot App", "Java");

        // Add a relation between the webApp container and the user
        user.uses(webApp, "Uses");

        Component mysqlComponent = null;

        // Try to connect to the database
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            System.out.println(" Successfully connected to DB: " + jdbcUrl);

            // Add the database as a component to the webApp container
            mysqlComponent = webApp.addComponent("MySQL Database", "Spring-managed MySQL datasource", "MySQL");
            mysqlComponent.getTags();

        } catch (Exception e) {
            System.out.println(" Could not connect to database: " + e.getMessage());
        }

        // Use reflection to introspect on the application
        Reflections reflections = new Reflections("com.example.demo");

        Map<Class<?>, Component> componentMap = new HashMap<>();

        // Add all controllers to the webApp component
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Controller.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Controller", "Java");
            componentMap.put(clazz, c);
            user.uses(c, "Uses");
        }

        // Add all services to the webApp component
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Service.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Service", "Java");
            componentMap.put(clazz, c);
        }

        // Add all repositories to the webApp component
        for (Class<?> clazz : reflections.getTypesAnnotatedWith(Repository.class)) {
            Component c = webApp.addComponent(clazz.getSimpleName(), "Repository", "Java");
            componentMap.put(clazz, c);
            if (mysqlComponent != null) {
                c.uses(mysqlComponent, "Reads/Writes data");
            }
        }

        // Auto-link via field injection
        for (Map.Entry<Class<?>, Component> entry : componentMap.entrySet()) {
            Class<?> sourceClass = entry.getKey();
            Component sourceComponent = entry.getValue();

            for (Field field : sourceClass.getDeclaredFields()) {
                Component targetComponent = componentMap.get(field.getType());
                if (targetComponent != null) {
                    sourceComponent.uses(targetComponent, "Uses via field: " + field.getName());
                }
            }
        }

        // Create views
        ContainerView containerView = views.createContainerView(system, "containers", "Container view");
        containerView.addAllElements();

        ComponentView componentView = views.createComponentView(webApp, "components", "Component view");
        componentView.addAllComponents();
        componentView.add(user);

        // Define styles
        Styles styles = views.getConfiguration().getStyles();
        styles.addElementStyle(Tags.PERSON).background("#08427b").color("#ffffff").shape(Shape.Person);
        styles.addElementStyle("Component").background("#85bb65").color("#ffffff").shape(Shape.Hexagon);
        styles.addElementStyle("Database").background("#f5da55").color("#000000").shape(Shape.Cylinder);

        // Export the model
        try (Writer writer = new FileWriter("workspace.json")) {
            new JsonWriter(true).write(workspace, writer);
            System.out.println("C4 model exported to workspace.json");
        }
    }
}