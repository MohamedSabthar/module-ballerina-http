import ballerina/http;

service class A {
    *http:Service;

    @http:ResourceConfig {
        cors: {
            allowOrigins: ["*"]
        }
    }
    resource function default .() returns string {
        return "";
    }

    resource function default greet() returns string {
        return "";
    }
};
