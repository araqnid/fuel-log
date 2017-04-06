define(['lodash'], function Bus$$init(_) {
    class Bus {
        constructor(name) {
            this.name = name;
            this.subscribers = { dead: [] }
        }
        broadcast(type, data) {
            let typeSubscribers = this.subscribers[type];
            if (typeSubscribers === undefined || typeSubscribers.length === 0) {
                typeSubscribers = this.subscribers.dead;
                if (console && console.log) console.log((this.name ? this.name : "BUS"), type + " (dead)", data);
            } else {
                if (console && console.log) console.log((this.name ? this.name : "BUS"), type, data);
            }
            _.forEach(typeSubscribers, subscriber => {
                const receiver = subscriber[0];
                const actor = subscriber[1];
                receiver.call(actor, data);
            });
        }
        subscribe(type, receiver, actor) {
            if (this.subscribers[type] === undefined) {
                this.subscribers[type] = [];
            }
            this.subscribers[type].push([receiver, actor]);
        }
        unsubscribe(actor) {
            _.forOwn(this.subscribers, (typeSubscribers, type) => {
                _.remove(typeSubscribers, subscriber => subscriber[1] === actor);
            });
        }
        isEmpty() {
            const counts = [];
            _.forOwn(this.subscribers, (typeSubscribers, type) => {
                counts.push(typeSubscribers.length);
            });
            return _.sum(counts) === 0;
        }
    }
    return Bus;
});
