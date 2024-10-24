import ballerina/http;

type AB service object {
    *http:Service;
    resource function default .() returns string;

    resource function default greet() returns string;
};
