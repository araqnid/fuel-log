import $ from "jquery";

$(window).hashchange(() => {
    BUS.broadcast("Router.HashChange", window.location.hash);
});
class Router {
    resync() {
        $(window).hashchange();
    }
};
export default new Router();
