if (!isMobile) {

    var panel = $("#style-changer"),
        arrow = panel.find(".sliding-panel-arrow"),
        navc = $("#nav"),
        pg = $("#page"),
        hsBtn = $("#home-section").find(".btn"),
        footr = $(".footer-section"),
        navw = $("#nav-wrapper");

    arrow.click(function() {
        panel.toggleClass('sh')
    });


    $('.color-changer a').click(function() {
        var color = $(this).attr('data-color');
        $('#theme').attr('href', 'css/theme/' + color + '.css');
    });


    if (navc.hasClass("navbar-default")) {
        $(".d-nav").removeClass("disabled");
        $(".l-nav").addClass("disabled");
    } else {

        $(".l-nav").removeClass("disabled");
        $(".d-nav").addClass("disabled");
    }


    if (pg.hasClass("light")) {
        $(".light-m").addClass("disabled");
        $(".dark-m").removeClass("disabled");
    } else {
        $(".dark-m").addClass("disabled");
        $(".light-m").removeClass("disabled");
    }



    if (pg.hasClass("equal")) {
        $(".equal-m").addClass("disabled");
        $(".diff-m").removeClass("disabled");
    } else {
        $(".diff-m").addClass("disabled");
        $(".equal-m").removeClass("disabled");
    }


    if (footr.hasClass("light-footer")) {
        $(".light-f").addClass("disabled");
        $(".dark-f").removeClass("disabled");
    } else {
        $(".dark-f").addClass("disabled");
        $(".light-f").removeClass("disabled");
    }


    $(".d-nav").on("click", function() {
        if (navc.hasClass("navbar-default")) {
            navc.removeClass("navbar-default").addClass("navbar-dark");
            $(".l-nav").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });
    $(".l-nav").on("click", function() {
        if (navc.hasClass("navbar-dark")) {
            navc.removeClass("navbar-dark").addClass("navbar-default");
            $(".d-nav").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });
    $(".light-m").on("click", function() {
        if (!pg.hasClass("light")) {
            pg.addClass("light");
            if (hsBtn.hasClass('btn-transparent-white')) {
                hsBtn.removeClass('btn-transparent-white').addClass('btn-transparent-dark')
            }
            if (hsBtn.hasClass('btn-white-transparent')) {
                hsBtn.removeClass('btn-white-transparent').addClass('btn-dark-transparent')
            }
            $(".dark-m").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });
    $(".dark-m").on("click", function() {
        if (pg.hasClass("light")) {
            pg.removeClass("light");
            if (hsBtn.hasClass('btn-transparent-dark')) {
                hsBtn.removeClass('btn-transparent-dark').addClass('btn-transparent-white')
            }
            if (hsBtn.hasClass('btn-dark-transparent')) {
                hsBtn.removeClass('btn-dark-transparent').addClass('btn-white-transparent')
            }
            $(".light-m").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });

    $(".equal-m").on("click", function() {
        if (!pg.hasClass("equal")) {
            pg.addClass("equal");
            $(".diff-m").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });
    $(".diff-m").on("click", function() {
        if (pg.hasClass("equal")) {
            pg.removeClass("equal");
            $(".equal-m").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });


    $(".light-f").on("click", function() {
        if (!footr.hasClass("light-footer")) {
            footr.addClass("light-footer");
            $(".dark-f").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });
    $(".dark-f").on("click", function() {
        if (footr.hasClass("light-footer")) {
            footr.removeClass("light-footer");
            $(".light-f").removeClass("disabled");
            $(this).addClass("disabled");
        }

    });


}
