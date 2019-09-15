import React from "react";
import {mount} from "enzyme";
import MockAdapter from "axios-mock-adapter";
import Root, {IdentityStoreContext} from "../../main/javascript/Root";
import Identity from "../../main/javascript/Identity";
import * as Ajax from "../../main/javascript/util/Ajax";
import IdentityStore from "../../main/javascript/identity/IdentityStore";

let mockAxios;
let identityStore;

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

beforeEach(() => {
    identityStore = new IdentityStore();
});

it("shows root page", () => {
    component = mount(<IdentityStoreContext.Provider value={identityStore}><Root/></IdentityStoreContext.Provider>);
    expect(component.find(Identity).length).toBe(1);
});
