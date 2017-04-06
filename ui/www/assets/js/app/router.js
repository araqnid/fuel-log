define(['jquery', 'jquery.ba-hashchange'], function router$$init($) {
    $(window).hashchange(() => {
        BUS.broadcast("Router.HashChange", window.location.hash);
    });
    class Router {
        resync() {
            $(window).hashchange();
        }
    }
    return new Router();
});
