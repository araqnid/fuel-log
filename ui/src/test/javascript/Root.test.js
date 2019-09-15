import React from "react";
import {mount} from "enzyme";
import MockAdapter from "axios-mock-adapter";
import Root from "../../main/javascript/Root";
import Identity from "../../main/javascript/Identity";
import * as Ajax from "../../main/javascript/util/Ajax";

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

let component = null;

afterEach(() => {
    if (component)
        component.unmount();
});

it("shows root page", () => {
    component = mount(<Root/>);
    expect(component.find(Identity).length).toBe(1);
});
