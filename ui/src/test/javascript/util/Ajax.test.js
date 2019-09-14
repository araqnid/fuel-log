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
    it("executes a GET request", async () => {
        mockAxios.onGet("/api/example").reply(200, { responseData: true });
        const responses = [];
        await Ajax.get("/api/example").forEach(it => responses.push(it));
        expect(responses[0]).toEqual({ responseData: true });
        expect(responses.length).toBe(1);
    });
});
