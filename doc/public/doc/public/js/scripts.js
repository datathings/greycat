// /*global $:false */
// /*global moment:true */
(function($) {

    "use strict";

    // $window.GLOB = {};
    var $window = $(window),
        $winWidth = $window.width(),
        $page = $("#page");

    // ==============================================
    // Auto Close Responsive Navbar on Click for small devices
    // =============================================== 
    var $MobileNav = $("#mobile-navbar-collapse"),
        $MobileNavAnchor = $MobileNav.find("a:not(.dropdown-toggle)");

    function init_autoclose_navbar() {
        if ($winWidth <= 767) {
            $MobileNavAnchor.on("click", function() {
                $MobileNav.collapse("hide");
                $animatedNavicon.removeClass("opened");
            });
        } else {
            $MobileNavAnchor.off("click");
        }
    }

    // ======================================
    // Aniamte navicon class toggler
    // ======================================
    var $animatedNavicon = $("#animated-navicon");

    function init_animate_navIcon() {
        $animatedNavicon.on("click", function() {
            $animatedNavicon.toggleClass("opened");
        });
    }

    // ==============================================
    //     Collapse transparent and elastic navbar
    // ==============================================
    var $nav = $("#nav"),
        $navWrapper = $("#nav-wrapper");


    function init_transparent_nav() {
        if ($navWrapper.hasClass("transp-nav")) {
            if ($window.scrollTop() < 130) {
                $nav.addClass("navbar-transparent");
            } else {
                $nav.removeClass("navbar-transparent");
            }
        }
        $window.scroll(function() {
            var scroll = $window.scrollTop();
            if ($navWrapper.hasClass("transp-nav")) {
                if (scroll < 130) {
                    $nav.addClass("navbar-transparent");
                } else {
                    $nav.removeClass("navbar-transparent");
                }
            }
            if ($navWrapper.hasClass("elastic-nav")) {
                if (scroll < 1) {
                    $nav.addClass("navbar-elastic");
                } else {
                    $nav.removeClass("navbar-elastic");
                }
            }
        });
    }

    // ======================================
    // Affix the navbar and portfolioNav after scroll below header
    // ======================================
    var $stickyNavWrapp = $(".sticky-navbar"),
        $stickyNav = $stickyNavWrapp.find('#nav'),
        $stickyVisible = $(".sticky-navbar"),
        $startOffset = $("#about-section"),
        $navbarHeight = $stickyNav.height();

    function init_sticky_nav() {
        if ($stickyNavWrapp.length) {
                $startOffset.addClass("sticky-nav-here");
            if ($stickyNavWrapp.hasClass("transp-nav")) {
                $startOffset.addClass("sticky-visible-here");

            }
            $stickyNav.affix({
                offset: {
                    top: $startOffset.offset().top + 1.2 * ($navbarHeight)
                }
            });
        }
    }
    var $portfolioNav = $("#portfolio-nav");

    function init_sticky_portfolio_nav() {
        if ($portfolioNav.length) {
            $portfolioNav.affix({
                offset: {
                    top: $portfolioNav.offset().top - 30
                }
            });
        }
    }

    // ======================================
    // animate-slideer
    // ======================================
    var $animateSlider = $("#animate-slider");

    function init_animateslider() {
        if ($animateSlider.length) {
            $animateSlider.animateSlider(8000);
            $(".show-indicators .as-indicators").addClass("dotstyle-fall");
        }
    }

    // ======================================
    //  Youtube Background Video
    // ======================================
    var $player = $(".player");

    function init_YTPlayer() {
        if ($player.length) {
            $(function() {
                $player.YTPlayer();
            });
        }
    }

    // ======================================
    //      Fluid Particles
    // ======================================
    function init_particlesA() {
        var $particlsA = $("#particles");
        if ($particlsA.length) {
            $particlsA.particleground({
                minSpeedX: 0.6,
                minSpeedY: 0.6,
                dotColor: '#ffffff',
                lineColor: '#ffffff',
                density: 6000,
                particleRadius: 2, // curvedLines: true,
                parallaxMultiplier: 5.2,
                proximity: 0
            });
        }
    }

    // ======================================
    //      Portfilio
    // ======================================
    var $isotopeContainer = $("#portfolio-container"),
        $isotopeFilters = $("#portfolio-filters"),
        $isotopeFilter = $isotopeFilters.find('a'),
        filterValue = 0;

    function init_isotope() {
        if ($isotopeContainer.length) {
            (function($) {
                "use strict";
                // 
                // setting the layout mode
                var isotope_mode;
                if ($isotopeContainer.hasClass("masonry")) {
                    isotope_mode = "masonry";
                } else {
                    isotope_mode = "fitRows";
                }
                // initial state
                $(window).load(function() {
                    $isotopeContainer.isotope({
                        // options
                        itemSelector: ".portfolio-item",
                        layoutMode: isotope_mode,
                        filter: filterValue
                    });
                });
                // when filter are clicked
                $isotopeFilters.on("click", "a", function() {
                    var $this = $(this);
                    // setting the selected button
                    $isotopeFilter.removeClass("selected");
                    $this.addClass("selected");
                    // setting the filter
                    filterValue = $this.attr("data-filter");
                    $isotopeContainer.isotope({
                        // options
                        itemSelector: ".portfolio-item",
                        layoutMode: isotope_mode,
                        filter: filterValue,
                        transitionDuration: ".5s"
                    });
                });
            })(jQuery);
        }
    }

    // ==============================================
    // Nivo LightBox
    // =============================================== 
    var $lightbox = $page.find(".lightbox");

    function init_lightbox() {
        if ($lightbox.length) {
            $lightbox.nivoLightbox({
                effect: "slideUp", // The effect to use when showing the lightbox
                theme: "default", // The lightbox theme to use
                keyboardNav: true, // Enable/Disable keyboard navigation (left/right/escape)
                clickOverlayToClose: true, // If false clicking the "close" button will be the only way to close the lightbox
                errorMessage: "The requested content cannot be loaded. Please try again later." // Error message when content can't be loaded
            });
        }
    }

    // ======================================
    // Owl carousel
    // ======================================
    function init_owl_sliders() {
        // Home slider | 1
        // -------------
        $("#owl-hs-slider-zoom").owlCarousel({
            autoplay: false,
            animateIn: 'zoomOutIn',
            animateOut: 'zoomInOut',
            loop: false,
            margin: 0,
            items: 1,
            rewind: true,
            dots: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Home slider | 2
        // -------------
        $("#owl-hs-slider").owlCarousel({
            // fluidSpeed: true,
            autoplay: false, // autoplaySpeed:1000,
            // autoplayTimeout: 8000,
            // animateIn: 'zoomOutIn',
            // animateOut: 'zoomInOut',
            // stagePadding:30,
            // smartSpeed:450
            loop: false,
            margin: 0, // stagePadding:15,
            // nav: true,
            // autoplayHoverPause: true,
            items: 1,
            smartSpeed: 1000,
            rewind: true, // center: false,
            // autoWidth: true,
            dots: true, // mouseDrag: false,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Home slider | 3
        // -------------
        $("#owl-hs-slider-text").owlCarousel({
            animateOut: 'zoomOut',
            animateIn: 'flipInX',
            margin: 0,
            items: 1,
            smartSpeed: 500,
            loop: true,
            dots: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Home slider | 4
        // -------------
        $("#owl-hs-slider-zoom-out").owlCarousel({
            autoplay: true,
            autoplayTimeout: 4000,
            animateIn: 'fadeIn',
            animateOut: 'fadeOut',
            loop: true,
            margin: 0,
            items: 1,
            // rewind: true,
            dots: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Images slider | 1
        // -------------
        var $imgSlider2 = $("#images-slider-1");
        $imgSlider2.owlCarousel({
            autoplay: true,
            autoplaySpeed: 1000,
            autoplayTimeout: 2000,
            loop: false,
            margin: 0,
            stagePadding: 0,
            autoplayHoverPause: false,
            items: 1,
            smartSpeed: 500,
            rewind: true,
            center: false,
            dots: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Images slider | 2
        // -------------
        var $imgSlider1 = $("#images-slider-2");
        $imgSlider1.owlCarousel({
            autoplay: true,
            autoplaySpeed: 1000,
            autoplayTimeout: 2000,
            loop: true,
            margin: 0,
            stagePadding: 0,
            autoplayHoverPause: false,
            items: 1,
            smartSpeed: 500,
            rewind: true,
            center: false,
            dots: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Team slider
        // -------------
        var $teamCarousel = $("#team-carousel");
        $teamCarousel.owlCarousel({
            autoplay: true,
            autoplaySpeed: 1000,
            autoplayTimeout: 5000,
            loop: false,
            margin: 0,
            nav: false,
            autoplayHoverPause: true,
            smartSpeed: 200,
            rewind: true,
            center: false,
            dots: true,
            mouseDrag: true,
            responsive: {
                0: {
                    items: 1
                },
                667: {
                    items: 2
                }
            }
        });

        var $teamCarousel3 = $("#team-carousel-3");
        $teamCarousel3.owlCarousel({
            // fluidSpeed: true,
            autoplay: true,
            autoplaySpeed: 1000,
            autoplayTimeout: 5000,
            // animateIn: 'fadeIn',
            // animateOut: 'fadeOut',
            // stagePadding:30,
            // merge: true,
            // smartSpeed:450
            loop: true,
            margin: 0, // stagePadding:15,
            autoplayHoverPause: true, // items: 6,
            // smartSpeed: 200, // stagePadding: 200,
            // rewind: false,
            center: false, // autoWidth: true,
            dots: true,
            nav: false,
            mouseDrag: true,
            responsive: {
                0: {
                    items: 1
                },
                667: {
                    items: 2
                },
                992: {
                    items: 3
                }
            }
        });

        // Testimonials slider
        // -------------
        var $owlTestimon = $("#testimonials");
        $owlTestimon.owlCarousel({
            animateIn: 'flipInX',
            animateOut: 'zoomOut',
            smartSpeed: 500,
            items: 1,
            loop: true,
            center: true,
            mouseDrag: true,
            margin: 10,
            autoplay: false,
            autoplayTimeout: 4000,
            autoplayHoverPause: true,
            responsive: {
                0: {
                    nav: false
                },
                768: {
                    nav: true
                }
            }
        });

        // Clients Logos
        // -------------
        var $owlClients = $("#client-logos");
        $owlClients.owlCarousel({
            autoplay: true,
            autoplaySpeed: 1000,
            autoplayTimeout: 2000,
            loop: false,
            stagePadding: 0,
            autoplayHoverPause: false,
            smartSpeed: 1200,
            rewind: true,
            center: false,
            dots: false,
            responsive: {
                0: {
                    items: 2,
                    margin: 20
                },
                480: {
                    items: 3,
                    margin: 70
                },
                768: {
                    items: 4,
                    margin: 70
                },
                992: {
                    items: 4,
                    margin: 100
                },
                1200: {
                    items: 4,
                    margin: 120
                }
            }
        });

        // Applying Dots And arrows Styles
        // -------------
        var $carousel = $(".carousel"),
            $carouselDots = $carousel.find(".owl-dots"),
            $carouselNav = $carousel.find(".owl-nav:not(.disabled)"),
            $carouselPrev = $carouselNav.find(".owl-prev"),
            $carouselNext = $carouselNav.find(".owl-next");
        $carouselDots.addClass("dotstyle-fall");
        $carouselPrev.addClass("left carousel-control").empty().append("<i class='arrow-left'></i>");
        $carouselNext.addClass("right carousel-control").empty().append("<i class='arrow-right'></i>");
        // }
    }

    // ======================================
    // Owl Carousel Process line
    // =====================================
    // 
    // 
    function init_process_slider() {
        var $owlCarouselProcess = $("#process");
        $owlCarouselProcess.owlCarousel({
            items: 1,
            smartSpeed: 500,
            nav: false,
            margin: 10,
            mouseDrag: true,
            autoplayTimeout: 4000,
            autoplayHoverPause: true
        });
        var $owlCarouselDots = $owlCarouselProcess.find(".owl-dots"),
            $processLabes = $("#process-section").find(".process-labels"),
            $processLable = $processLabes.find("li"),
            $owlCarouselDot = $owlCarouselDots.find(".owl-dot");
        // applying fill dot styles
        $owlCarouselDots.addClass("dotstyle-fillup line-process-mood");
        // Creating process line
        // getting "data-que" which we have assigned to teh label and attaching them to te dots 
        var looper = 1;
        $owlCarouselDot.each(function() {
            $(this).attr('data-que', looper);
            looper += 1;
        });
        // positioning the dots
        var perci = 100 / (looper - 2),
            widthLooper = 0;
        $owlCarouselDot.each(function() {
            $(this).css({
                left: widthLooper * perci + "%"
            });
            widthLooper += 1;
        });
        // positioning the labels
        var labelLooper = 0;
        $processLable.each(function() {
            var $this = $(this),
                $width = $this.find("span").width();
            $this.css({
                left: labelLooper * perci + "%",
                "margin-left": (-$width / 2)
            });
            labelLooper += 1;
        });
        // applying the process line width
        $owlCarouselDot.on("owlDotClassChange", function() {
            var lineWidth = $(this).attr('data-que') - 1,
                lineProcess = $(".line-process"),
                $owlCarouselDotActive = $owlCarouselDots.find(".owl-dot.active");
            lineProcess.width(perci * lineWidth + "%");
            // making the previous dots stay filled
            $owlCarouselDot.each(function() {
                var $this = $(this);
                if ($this.attr('data-que') < $owlCarouselDotActive.attr('data-que')) {
                    $this.children("span").addClass("process-active-dot");
                } else {
                    $this.children("span").removeClass("process-active-dot");
                }
            });
            $processLable.each(function() {
                var $this = $(this);
                if ($this.attr('data-que') == $owlCarouselDotActive.attr('data-que')) {
                    $this.addClass("process-label-active");
                } else {
                    $this.removeClass("process-label-active");
                }
            });
        });
        var $carousel = $(".carousel"),
            $carouselPrev = $carousel.find(".owl-prev"),
            $carouselNext = $carousel.find(".owl-next");
        // applying arrows styles
        $carouselPrev.addClass("left carousel-control").empty().append("<i class='arrow-left'></i>");
        $carouselNext.addClass("right carousel-control").empty().append("<i class='arrow-right'></i>");
    }

    // ======================================
    // Skill bars
    // ======================================
    var $skillBars = $("#skillbars"),
        $skillBar = $skillBars.find(".skillbar-bar"),
        $allSklBrs = $(".skillbar-bar");

    function init_skillBars() {
        if ($skillBars.length) {
            if ($winWidth >= 768) {
                $skillBars.waypoint(function() {
                    $skillBar.each(function() {
                        var $this = $(this);
                        $this.width($this.data("percent"));
                    });
                }, {
                    offset: "60%"
                });
            } else {
                $allSklBrs.each(function() {
                    var $this = $(this);
                    $this.width($this.data("percent"));
                });
            }
        }
    }

    // ==============================================
    //     Google Maps
    // ==============================================
    //add custom buttons for the zoom-in/zoom-out on the map
    function CustomZoomControl(controlDiv, map) {
        //grap the zoom elements from the DOM and insert them in the map 
        var controlUIzoomIn = document.getElementById("map-zoom-in"),
            controlUIzoomOut = document.getElementById("map-zoom-out");
        controlDiv.appendChild(controlUIzoomIn);
        controlDiv.appendChild(controlUIzoomOut);
        // Setup the click event listeners and zoom-in or out according to the clicked element
        google.maps.event.addDomListener(controlUIzoomIn, "click", function() {
            map.setZoom(map.getZoom() + 1);
        });
        google.maps.event.addDomListener(controlUIzoomOut, "click", function() {
            map.setZoom(map.getZoom() - 1);
        });
    }

    function init_map() {
        if ($("#map-section").length) {
            // -------------------------------------
            // map opener
            // -------------------------------------
            var $mapOpener = $("#map-opener"),
                $mapSection = $("#map-section");
            $mapOpener.on("click", function() {
                $mapSection.toggleClass("map-opened");
            });
            var mapZoom = 14, // mapLatitude = 40.679418,
                // mapLongitude = -73.886275,
                //define the basic color of your map, plus a value for saturation and brightness
                main_color = "#111518", // saturation_value = -20,
                // brightness_value = 5,
                // // First marker position
                // markerLatitude = 40.679418,
                // markerLongitude = -73.886275,
                // First marker popup structure and content 
                firstContentString = "<div class='gmap-popup'>" + "<h4>" + "Our primary office" + "</h4>" + "<p>" + "Your notes sits here!" + "</p>" + "</div>";
            // // Second marker position
            // secondMarkerLatitude = 40.68000,
            // secondMarkerLongitude = -73.901525,
            // // Second marker popup structure and content 
            // secondContentString =
            // "<div class='gmap-popup'>" +
            // "<h4>" +
            // "Our secondary office" +
            // "</h4>" +
            // "<p>" +
            // "Your notes sits here!" +
            // "</p>" +
            // "</div>";
            // google map custom marker icon - .png fallback for IE11
            var is_internetExplorer11 = navigator.userAgent.toLowerCase().indexOf('trident') > -1;
            var markerIcon = (is_internetExplorer11) ? 'img/google-map-assets/map-icon-location.png' : 'img/google-map-assets/map-icon-location.svg';
            // we define here the style of the map
            var mapStyles = [{
                "featureType": "landscape",
                "stylers": [{
                    "saturation": -100
                }, {
                    "lightness": 65
                }, {
                    "visibility": "on"
                }]
            }, {
                "featureType": "poi",
                "stylers": [{
                    "saturation": -100
                }, {
                    "lightness": 51
                }, {
                    "visibility": "simplified"
                }]
            }, {
                "featureType": "road.highway",
                "stylers": [{
                    "saturation": -100
                }, {
                    "visibility": "simplified"
                }]
            }, {
                "featureType": "road.arterial",
                "stylers": [{
                    "saturation": -100
                }, {
                    "lightness": 30
                }, {
                    "visibility": "on"
                }]
            }, {
                "featureType": "road.local",
                "stylers": [{
                    "saturation": -100
                }, {
                    "lightness": 40
                }, {
                    "visibility": "on"
                }]
            }, {
                "featureType": "transit",
                "stylers": [{
                    "saturation": -100
                }, {
                    "visibility": "simplified"
                }]
            }, {
                "featureType": "administrative.province",
                "stylers": [{
                    "visibility": "off"
                }]
            }, {
                "featureType": "water",
                "elementType": "labels",
                "stylers": [{
                    "visibility": "on"
                }, {
                    "lightness": -25
                }, {
                    "saturation": -100
                }]
            }, {
                "featureType": "water",
                "elementType": "geometry",
                "stylers": [{
                    "hue": main_color
                }, {
                    "lightness": -25
                }, {
                    "saturation": -97
                }]
            }];
            // initiate geocider
            var geocoder = new google.maps.Geocoder();
            // Address
            var address = $("#map-canvas").attr("data-address");
            // var address = document.getElementById('address').value;
            geocoder.geocode({
                'address': address
            }, function(results, status) {
                if (status == google.maps.GeocoderStatus.OK) {
                    //set google map options
                    var mapOptions = {
                        zoom: mapZoom,
                        center: results[0].geometry.location,
                        panControl: false,
                        zoomControl: false,
                        mapTypeControl: false,
                        streetViewControl: false,
                        mapTypeId: google.maps.MapTypeId.ROADMAP,
                        scrollwheel: false,
                        tilt: 45, // Color Styles
                        styles: mapStyles
                    };
                    //inizialize the map
                    var map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
                    //inizialize the first marker
                    var marker = new google.maps.Marker({
                        position: results[0].geometry.location,
                        map: map,
                        icon: markerIcon
                    });
                    var firstInfowindow = new google.maps.InfoWindow({
                        content: firstContentString
                    });
                    google.maps.event.addListener(marker, "click", function() {
                        firstInfowindow.open(map, marker);
                    });
                    var zoomControlDiv = document.createElement("div");
                    var zoomControl = new CustomZoomControl(zoomControlDiv, map);
                    //insert the zoom div on the top left of the map
                    map.controls[google.maps.ControlPosition.LEFT_TOP].push(zoomControlDiv);
                } else {
                    alert('Geocode was not successful for the following reason: ' + status);
                }
            });
        }
    }

    // ==============================================
    //   Animsition | Page loading effects
    // ==============================================
    var $animsition = $(".animsition");
    var initAnimation = function() {
        $(".init-animation-1").addClass("fadeInDown").css("opacity", 1);
        $(".init-animation-2").addClass("fadeInUp").css("opacity", 1);
        $(".init-animation-3").addClass("fadeInRight").css("opacity", 1);
        $(".init-animation-4").addClass("fadeInLeft").css("opacity", 1);
        $(".init-animation-5").addClass("fadeIn").css("opacity", 1);
    };

    function init_animsition() {
        if ($animsition.length) {
            $animsition.animsition({
                // inClass               :   'fade-in',
                // outClass              :   'fade-out',
                inDuration: 1200,
                outDuration: 800,
                linkElement: '.animsition-link', // e.g. linkElement   :   'a:not([target="_blank"]):not([href^=#])'
                loading: true,
                loadingParentElement: 'body', //animsition wrapper element
                loadingClass: 'animsition-loading',
                unSupportCss: ['animation-duration', '-webkit-animation-duration', '-o-animation-duration'], //"unSupportCss" option allows you to disable the "animsition" in case the css property in the array is not supported by your browser.
                //The default setting is to disable the "animsition" in a browser that does not support "animation-duration".
                overlay: false,
                overlayClass: 'animsition-overlay-slide',
                overlayParentElement: 'body'
            }).one('animsition.start', function() {
                setTimeout(initAnimation, 800);
            }).one('animsition.end', function() {
                init_skillBars();
                init_skrollr();
            });
        }
    }

    // ======================================
    //         Skrollr
    // ======================================
    function init_skrollr() {
        if (!isMobile) {
            // Init Skrollr
            var s = skrollr.init({
                forceHeight: false,
                smoothScrolling: false
            });
        }
    }


    /*|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||

      =======      Below are functions that are dedicated only for large devices     ========

      |||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||*/
    if (!isMobile && screen.width >= 768) {

        // ==============================================
        //    WOW | Animation on scroll
        // ==============================================
        var wow = new WOW({
                boxClass: 'wow', // animated element css class (default is wow)
                animateClass: 'animated', // animation css class (default is animated)
                offset: 0, // distance to the element when triggering the animation (default is 0)
                mobile: false, // trigger animations on mobile devices (default is true)
                live: true // act on asynchronously loaded content (default is true)
            }),

            // ==============================================
            //     Ripple effect
            // ==============================================
            //Ripple effect for group buttons and menues
            elem, ink, d, x, y, $rippleGroup = $(".ripple-group"),
            $rippleAlone = $(".ripple-alone"),
            init_ripple = function() {
                $rippleGroup.on("click", function(e) {
                    parent = $(this).parents(".ripple-group-parent");
                    //create .ink element if it doesn't exist
                    if (parent.find(".ink").length === 0) {
                        parent.append("<span class='ink'></span>");
                    }
                    ink = parent.find(".ink");
                    //incase of quick double clicks stop the previous animation
                    ink.removeClass("animate");
                    //set size of .ink
                    if (!ink.height() && !ink.width()) {
                        //use parent's width or height whichever is larger for the diameter to make a circle which can cover the entire element.
                        d = Math.max(parent.outerWidth(), parent.outerHeight());
                        ink.css({
                            height: d,
                            width: d
                        });
                    }
                    //get click coordinates
                    //logic = click coordinates relative to page - parent's position relative to page - half of self height/width to make it controllable from the center;
                    x = e.pageX - parent.offset().left - ink.width() / 2;
                    y = e.pageY - parent.offset().top - ink.height() / 2;
                    //set the position and add class .animate
                    ink.css({
                        top: y + 'px',
                        left: x + 'px'
                    }).addClass("animate");
                });
                //Ripple effect for single elements
                $rippleAlone.on("click", function(e) {
                    elem = $(this);
                    //create .ink element if it doesn't exist
                    if (elem.find(".ink").length === 0) {
                        elem.append("<span class='ink'></span>");
                    }
                    ink = elem.find(".ink");
                    //incase of quick double clicks stop the previous animation
                    ink.removeClass("animate");
                    //set size of .ink
                    if (!ink.height() && !ink.width()) {
                        //use elem's width or height whichever is larger for the diameter to make a circle which can cover the entire element.
                        d = Math.max(elem.outerWidth(), elem.outerHeight());
                        ink.css({
                            height: d,
                            width: d
                        });
                    }
                    //get click coordinates
                    //logic = click coordinates relative to page - elem's position relative to page - half of self height/width to make it controllable from the center;
                    x = e.pageX - elem.offset().left - ink.width() / 2;
                    y = e.pageY - elem.offset().top - ink.height() / 2;
                    //set the position and add class .animate
                    ink.css({
                        top: y + 'px',
                        left: x + 'px'
                    }).addClass("animate");
                });
            },

            // ======================================
            // Smooth scrolling for in page links
            // ======================================
            $root = $("html, body"),
            $smoothScrollAnchor = $(".in-page-scroll").find("a[href*=#]"),
            init_inpage_scroll = function() {
                $smoothScrollAnchor.on("click", function(event) {
                    $root.animate({
                        scrollTop: $($.attr(this, "href")).offset().top
                    }, 2000, "easeInCubic");
                    event.preventDefault();
                });
            },

            // ==============================================
            //     Page scroll progress bar
            // ==============================================
            $scrollProgressBar = $("#scroll-progressbar").find("div"),
            init_page_scrollBar = function() {
                $window.scroll(function() {
                    var value = $(document).scrollTop(),
                        max = $(document).height() - $window.height();
                    $scrollProgressBar.width((value / max) * 100 + "%");
                });
            },

            // ==============================================
            //     Blur on scroll header
            // ==============================================
            $blur = $("#blur"),
            init_blurScrl = function() {
                if ($blur.length) {
                    var blH = $blur.height(),
                        blHS = blH + 1,
                        blurVal = $blur.attr("data-blur");
                    var attrs = {};
                    attrs['data-top'] = 'filter: blur(0px); translate3d(0px,0px,0px);';
                    attrs['data--' + blH + '-top'] = 'filter: blur(' + blurVal + ' ); translate3d(0px,0px,0px);';
                    attrs['data--' + blHS + '-top'] = 'filter: blur(0px); translate3d(0px,0px,1px);';
                    $blur.attr(attrs);
                }
            },

            // ======================================
            // Sticky Particles
            // ======================================
            init_particlesB = function() {
                if ($("#particles-js").length) {
                    particlesJS('particles-js', {
                        "particles": {
                            "number": {
                                "value": 110,
                                "density": {
                                    "enable": true,
                                    "value_area": 800
                                }
                            },
                            "color": {
                                "value": "#ffffff"
                            },
                            "shape": {
                                "type": "circle",
                                "stroke": {
                                    "width": 0,
                                    "color": "#ffffff"
                                },
                                "polygon": {
                                    "nb_sides": 5
                                },
                                "image": {
                                    "src": "img/github.svg",
                                    "width": 100,
                                    "height": 100
                                }
                            },
                            "opacity": {
                                "value": 0.6000850120433731,
                                "random": true,
                                "anim": {
                                    "enable": false,
                                    "speed": 1,
                                    "opacity_min": 0.1,
                                    "sync": false
                                }
                            },
                            "size": {
                                "value": 1,
                                "random": true
                            },
                            "line_linked": {
                                "enable": true,
                                "distance": 220,
                                "color": "#ffffff",
                                "opacity": 0.4,
                                "width": 1
                            },
                            "move": {
                                "enable": true,
                                "speed": 8,
                                "direction": "none",
                                "random": true,
                                "straight": false,
                                "out_mode": "out",
                                "bounce": false,
                                "attract": {
                                    "enable": false,
                                    "rotateX": 600,
                                    "rotateY": 1200
                                }
                            }
                        },
                        "interactivity": {
                            "detect_on": "canvas",
                            "events": {
                                "onhover": {
                                    "enable": true,
                                    "mode": "grab"
                                },
                                "onclick": {
                                    "enable": true,
                                    "mode": "repulse"
                                },
                                "resize": true
                            },
                            "modes": {
                                "grab": {
                                    "distance": 260,
                                    "line_linked": {
                                        "opacity": 1
                                    }
                                },
                                "bubble": {
                                    "distance": 400,
                                    "size": 40,
                                    "duration": 2,
                                    "opacity": 8,
                                    "speed": 3
                                },
                                "repulse": {
                                    "distance": 180,
                                    "duration": 0.4
                                },
                                "push": {
                                    "particles_nb": 4
                                },
                                "remove": {
                                    "particles_nb": 2
                                }
                            }
                        },
                        "retina_detect": true
                    });
                }
            },
            // ======================================
            // Number Counter
            // ======================================
            $counter = $('.counter'),
            init_numberCounter = function() {
                if ($counter.length) {
                    $counter.counterUp({
                        delay: 10,
                        time: 800
                    });
                }
            },

            // ======================================
            //  Team Item skill Bars
            // ======================================
            $teamItem = $("#team-section").find(".team-item"),
            init_team_skillBars = function() {
                $teamItem.mouseenter(function() {
                    $(this).find(".skillbar-bar").each(function() {
                        var $this = $(this);
                        $this.width($this.attr("data-percent"));
                    });
                }).mouseleave(function() {
                    $teamItem.find(".skillbar-bar").width(0);
                });
            },

            // ======================================
            //   Go top button pop-up
            // ======================================
            $goTop = $("#go-top"),
            init_gotop_pop = function() {
                $window.scroll(function() {
                    if ($window.scrollTop() + $window.height() > $(document).height() - 200) {
                        $goTop.addClass("go-top-out");
                    } else {
                        $goTop.removeClass("go-top-out");
                    }
                });
            },

            // ======================================
            //   sticky sidebar
            // ======================================
            // 
            init_sticy_sidebar = function() {
                var sidebar = $('#sidebar');
                if (sidebar.length) {
                    var top = sidebar.offset().top - parseFloat(sidebar.css('margin-top').replace(/auto/, 0)),
                        height = sidebar.height(),
                        winHeight = $(window).height(),
                        winWidth = $(window).width(),
                        containerWidth = $(".container").width(),
                        right = (winWidth - containerWidth) / 2,
                        stopStick = $('#end-content').offset().top - parseFloat($('#end-content').css('margin-top').replace(/auto/, 0)) - 180,
                        gap = 30;
                    if (sidebar.hasClass('col-sm-3')) {
                        sidebar.outerWidth(containerWidth / 4);
                    } else if (sidebar.hasClass('col-sm-4')) {
                        sidebar.outerWidth(containerWidth / 3);
                    }
                    $(window).scroll(function() {
                        // what the y position of the scroll is
                        var y = $(this).scrollTop();
                        // whether that's below the form
                        if (y + winHeight >= top + height + gap && y + winHeight <= stopStick) {
                            // if so, ad the fixed class
                            sidebar.addClass('sidebarfixed').css({
                                'top': winHeight - height - gap + 'px',
                                "right": right + (15 / 2)
                            });
                        } else if (y + winHeight > stopStick) {
                            // if so, ad the fixed class
                            sidebar.addClass('sidebarfixed').css({
                                'top': stopStick - height - y - gap + 'px',
                                "right": right + (15 / 2)
                            });
                        } else {
                            // otherwise remove it
                            sidebar.removeClass('sidebarfixed').css({
                                'top': '0px',
                                "right": 15
                            });
                        }
                    });
                }
            };

        // ======================================
        // Refreshing Nav Status On Scroll
        // ======================================
        $root.each(function() {
            var $spy = $(this).scrollspy('refresh');
        });

        // init function on large devices
        init_page_scrollBar();
        init_ripple();
        init_gotop_pop();
        init_animate_navIcon();
        init_transparent_nav();
        init_sticky_nav();
        init_sticky_portfolio_nav();
        init_sticy_sidebar();
        wow.init();
        init_particlesB();
        init_team_skillBars();
        init_numberCounter();
        init_animateslider();
        init_YTPlayer();
        init_particlesA();
        init_lightbox();
        init_owl_sliders();
        init_isotope();
        init_inpage_scroll();
        init_process_slider();
        init_blurScrl();
        init_animsition();
        if (!$page.hasClass("animsition")) {
            init_skillBars();
            init_skrollr();
        }
        if (!isMobile) {
            // set the timer
            var timer;
            $window.resize(function() {
                init_sticy_sidebar();
                clearTimeout(timer);
                timer = setTimeout(function() {
                    // Preparing the skrollr to be triggered on resize
                    var s = skrollr.init({
                        forceHeight: false,
                        smoothScrolling: false
                    }).refresh();
                    init_blurScrl();
                }, 1200);
            });
        }
        init_map();
    } else {

        // init function on small devices
        init_transparent_nav();
        init_sticky_nav();
        init_sticky_portfolio_nav();
        init_animateslider();
        init_YTPlayer();
        init_particlesA();
        init_lightbox();
        init_autoclose_navbar();
        init_animate_navIcon();
        init_owl_sliders();
        if (!$page.hasClass("animsition")) {
            init_skillBars();
        }
        init_isotope();
        init_process_slider();
        init_animsition();
        init_map();
    }
})(jQuery);
