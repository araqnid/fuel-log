define(['lodash', 'app/Bus'], function MemoBus$$init(_, Bus) {
    class MemoBus {
        constructor(name) {
            this.memory = {};
            this.bus = new Bus(name);
        }
        dispatch(type, payload) {
            this.memory[type] = payload;
            this.bus.broadcast(type, payload);
        }
        subscribeAll(handlers, owner) {
            _.forOwn(handlers, (target, type) => {
                this.subscribe(type, target, owner);
            });
        }
        subscribe(type, target, owner) {
            this.bus.subscribe(type, target, owner);
            if (this.memory[type]) {
                target(this.memory[type]);
            }
        }
        unsubscribe(owner) {
            this.bus.unsubscribe(type, target, owner);
        }
        isEmpty() {
            return this.bus.isEmpty();
        }
    }
    return MemoBus;
});
