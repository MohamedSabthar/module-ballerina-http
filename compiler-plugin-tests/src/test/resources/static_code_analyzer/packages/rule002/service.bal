import ballerina/http;

@http:ServiceConfig {
    cors: {
        allowOrigins: ["*"]
    }
}
service on new http:Listener(8080) {

    @http:ResourceConfig {
        cors: {
            allowOrigins: ["*"]
        }
    }
    resource function default greet() returns string? {
        return;
    }

    resource function default .() returns string? {
        return;
    }
};
