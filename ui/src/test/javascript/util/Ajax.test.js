import MockAdapter from "axios-mock-adapter";
import * as Ajax from "../../../main/javascript/util/Ajax";

let mockAxios;

beforeAll(() => {
    mockAxios = new MockAdapter(Ajax.localAxios);
});

afterEach(() => {
    mockAxios.reset();
});

afterAll(() => {
    mockAxios.restore();
});

describe("get", () => {
    it("executes a GET request and returns data", async () => {
        mockAxios.onGet("/_api/example").reply(200, {responseData: true});
        const responses = [];
        await Ajax.get("example").forEach(it => responses.push(it));
        expect(responses[0]).toEqual({responseData: true});
        expect(responses.length).toBe(1);
    });
});

describe("postRow", () => {
    it("executes a POST request and returns response", async () => {
        mockAxios.onPost("/_api/example").reply(200, {responseData: true});
        const responses = [];
        await Ajax.postRaw("example", {v: 1}).forEach(it => responses.push(it));
        expect(responses[0].data).toEqual({responseData: true});
        expect(responses[0].status).toBe(200);
        expect(responses.length).toBe(1);
    });
});
