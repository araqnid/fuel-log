import React from "react";
import {mount} from "enzyme";
import {createStore} from "redux";
import {Provider} from "react-redux";
import Root from "../../main/javascript/Root";
import reducers from "../../main/javascript/reducers";
import Identity from "../../main/javascript/Identity";
import Content from "../../main/javascript/Content";

let component = null;

afterEach(() => {
    if (component)
        component.unmount();
});

it("shows root page", () => {
    const store = createStore(reducers);
    component = mount(<Provider store={store}><Root/></Provider>);
    expect(component.find(Identity).length).toBe(1);
    expect(component.find(Content).length).toBe(1);
});
