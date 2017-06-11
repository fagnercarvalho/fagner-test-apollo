package com.fagner.test.apollo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.apollo.Environment;
import com.spotify.apollo.Request;
import com.spotify.apollo.RequestContext;
import com.spotify.apollo.Response;
import com.spotify.apollo.Status;
import com.spotify.apollo.httpservice.HttpService;
import com.spotify.apollo.httpservice.LoadingException;
import com.spotify.apollo.route.AsyncHandler;
import com.spotify.apollo.route.Middleware;
import com.spotify.apollo.route.Route;
import com.spotify.apollo.route.RouteProvider;
import com.spotify.apollo.route.SyncHandler;
import com.typesafe.config.Config;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 *
 * @author Fagner
 */
public final class App {

    public static void main(String[] args) throws LoadingException {
        HttpService.boot(App::init, "test-apollo", args);
    }

    static void init(Environment environment) {
        Config testConfig = environment.config();
        String testConfigValue = testConfig.getString("test-apollo.testvalue");

        SyncHandler<Response<String>> echoHandler = context -> echo(context.request());
        SyncHandler<Response<String>> addTestHandler = context -> addTest(context.request());

        environment.routingEngine()
                .registerAutoRoute(Route.with(authorizationHandler(), "GET", "/echo", echoHandler))
                .registerAutoRoute(Route.sync("GET", "/", rc -> "Hello World!"))
                .registerAutoRoute(Route.sync("POST", "/addTest", addTestHandler))
                .registerAutoRoute(Route.sync("GET", "/ping", new PingHandler())
                        .withDocString("Ping pong", "Does a simple ping pong"))
                .registerAutoRoutes(new CustomerRoutes())
                .registerAutoRoute(Route.sync("GET", "/config", rc -> testConfigValue));
    }

    static Response<String> echo(Request request) {
        Optional<String> parameter = request.parameter("parameter");

        if (!parameter.isPresent()) {
            return Response.forStatus(Status.BAD_REQUEST);
        }

        String value = parameter.get();

        return Response.forPayload("Echo: " + value);

    }

    static Response<String> addTest(Request request) {
        if (!request.payload().isPresent()) {
            Response.forStatus(Status.BAD_REQUEST);
        }

        byte[] bytes = request.payload().get().toByteArray();

        Test test = null;
        try {
            test = (Test) convertJsonToObject(bytes, Test.class);
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (test == null) {
            return Response.forStatus(Status.BAD_REQUEST);
        }

        return Response.forPayload(test.toString());
    }

    static class PingHandler implements SyncHandler<String> {

        @Override
        public String invoke(RequestContext requestContext) {
            return "pong";
        }
    }

    static class CustomerRoutes implements RouteProvider {

        @Override
        public Stream<? extends Route<? extends AsyncHandler<?>>> routes() {
            return Stream.of(
                    Route.sync("GET", "/customer/<id>", ctx -> getCustomer(ctx.pathArgs().get("id"))),
                    Route.sync("POST", "/customer", ctx -> createCustomer(ctx))
            );
        }

        String getCustomer(String id) {
            return "get customer";
        }

        String createCustomer(RequestContext context) {
            return "add customer";
        }
    }

    static <T> Middleware<SyncHandler<Response<T>>, SyncHandler<Response<T>>> authorizationMiddleware() {
        return (SyncHandler<Response<T>> handler) -> (RequestContext requestContext) -> {

            Optional<String> header = requestContext.request().header("Authorization");

            if (!header.isPresent()) {
                return UnauthorizedResponse();
            }

            String authorization = header.get();

            String value = authorization
                    .replace("Basic", "")
                    .trim();

            if (value.isEmpty()) {
                return UnauthorizedResponse();
            }

            String credentials = "";
            try {
                credentials = new String(Base64.getDecoder().decode(value), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }

            if (credentials.isEmpty()) {
                return UnauthorizedResponse();
            }

            String[] splittedCredentials = credentials.split(":");

            if (splittedCredentials.length != 2) {
                return UnauthorizedResponse();
            }

            if (!splittedCredentials[0].equals("admin")
                    || !splittedCredentials[1].equals("admin123")) {
                return UnauthorizedResponse();
            }

            return handler.invoke(requestContext);
        };
    }

    static <T> Middleware<SyncHandler<Response<T>>, AsyncHandler<Response<T>>> authorizationHandler() {
        return App.<T>authorizationMiddleware().and(Middleware::syncToAsync);
    }

    static <T> Object convertJsonToObject(byte[] data, Class<T> classRef) throws IOException, ClassNotFoundException {
        ObjectMapper mapper = new ObjectMapper();
        T object = mapper.readValue(data, classRef);
        return object;
    }

    static <T> Response<T> UnauthorizedResponse() {
        return (Response<T>) Response.forStatus(Status.UNAUTHORIZED)
                .withHeader("WWW-Authenticate", "Basic realm=\"Access to the site\"");
    }
}
