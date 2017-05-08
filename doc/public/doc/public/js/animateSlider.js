// 1. load bars stacked beside eachother.
// 2. only one full-width load bar appears each slide.
// 3. no load bars just dots.
// 4. video background.
// 5. one back ground.


"use strict";
/*
 Plugin: jQuery AnimateSlider
 Version 1.0.0
 Author: John John
 */
(function($) {
    $.fn.animateSlider = function(slideDur) {
        this.each(function() {
            var $container = $(this);
            //Json Object for container
            $container[0].faderConfig = {};
            //Private vars
            var slideSelector = ".slide", //Slide selector
                slideTimer, //Timeout
                activeSlide, //Index of active slide
                newSlide, //Index of next or prev slide
                $slides = $container.find(slideSelector), //All slides
                totalSlides = $slides.length, //Nb of slides
                config = $container[0].faderConfig; //Configuration



            // change background function
            function changeBG() {
                if ($container.attr("data-background-1")) {
                    $(".as-background").each(function() {
                        var indexOfSlide = $(this).data("slide-num");
                        if (newSlide == (indexOfSlide)) {
                            $(this).css("opacity", 1).addClass("active-as-background");
                        } else {
                            $(this).css("opacity", 0).removeClass("active-as-background");
                        }
                    });
                }
            }


            config = {
                slideDur: slideDur
            };
            // make active dots function
            function activDot() {
                $(".as-indicator").each(function() {
                    var indexOfSlide = $(this).data("slide-number");
                    if (newSlide == indexOfSlide) {
                        $(this).addClass("active");
                    } else {
                        $(this).removeClass("active");
                    }
                });
            }


            function progress(percent, $element) {
                if (!$container.hasClass("show-indicators")) {
                    var progressBarPercent = percent * $element.width() / 100;
                    $element.find("span").animate({
                        width: progressBarPercent
                    }, slideDur);
                }
            }
            slideTimer = setTimeout(function() {
                changeSlides("next");
            }, config.slideDur);
            /**
             * Function to change slide
             * @param {type} target, next ou prev
             * @returns {undefined}
             */
            function changeSlides(target) {
                //If want to forward
                if (target === "next") {
                    //index of next slide
                    newSlide = activeSlide + 1;
                    if (newSlide > totalSlides - 1) {

                        newSlide = 0;

                        $(".as-indicator .as-load-bar").stop().width(0);
                    }

                } else if (target === "prev") {
                    newSlide = activeSlide - 1;

                    if (newSlide < 0) {
                        newSlide = totalSlides - 1;
                    }
                } else {
                    newSlide = target;
                }

                $(".slide" + newSlide).find(".as-load-bar").width(0);
                $(".slide" + activeSlide).find(".as-load-bar").stop().width(0);
                if (newSlide < totalSlides) {
                    $(".slide" + newSlide).prevAll().each(function(index, element) {
                        $(element).find(".as-load-bar").stop().css("width", "100%");
                    });
                }
                animateSlides(activeSlide, newSlide);
                // active the curret dot
                changeBG();
                activDot();

            }
            if (!$container.hasClass("single-loadbar")) {
                var fireEventProgressBar = false;
                //Change slide by clicking on progress bar
                $("body").delegate(".as-indicator", "click", function() {
                    if (!fireEventProgressBar) {
                        //To avoid spam clicks
                        fireEventProgressBar = true;
                        setTimeout(function() {
                            fireEventProgressBar = false;
                        }, 1000);
                        $this = $(this);
                        $this.find(".as-load-bar").stop().width(0);

                        var indexOfSlide = $this.data("slide-number");

                        if (indexOfSlide > activeSlide) {

                            $this.prevAll().each(function(index, element) {
                                $(element).find(".as-load-bar").stop().css("width", "100%");
                            });
                        } else if (indexOfSlide < activeSlide) {

                            $this.nextAll().each(function(index, element) {
                                $(element).find(".as-load-bar").stop().width(0);
                            });
                        }

                        newSlide = indexOfSlide;
                        changeBG();
                        // active the curret dot
                        activDot();
                        clearTimeout(slideTimer);

                        animateSlides(activeSlide, newSlide);
                    }
                });
                slideTimer;
            }

            var arrows = $container.find(".as-nav-arrows .as-arrow");
            if (arrows.length) {
                var fireEventArrow = false;
                arrows.on("click", function() {

                    if (!fireEventArrow) {
                        fireEventArrow = true;
                        setTimeout(function() {
                            fireEventArrow = false;
                        }, 1000);

                        var target = $(this).data("target");
                        clearTimeout(slideTimer);
                        changeSlides(target);
                    }
                });
                slideTimer;
            }
            /**
             * Animation of slides
             * @param {type} indexOfActiveSlide
             * @param {type} indexOfnewSlide
             * @returns {undefined}
             */
            function animateSlides(indexOfActiveSlide, indexOfnewSlide) {

                $slides.eq(indexOfActiveSlide).css("z-index", 5);

                var childsOfSlide = $slides.eq(indexOfActiveSlide).children();
                $(childsOfSlide).each(function(index, element) {
                    if (typeof $(element).data("effect-in") !== "undefined" && typeof $(element).data("effect-out") !== "undefined") {
                        $(element).removeClass($(element).data("effect-in") + " animated");
                        $(element).addClass($(element).data("effect-out") + " animated slide-out");
                    } else {
                        $(element).children().each(function(index, child) {
                            if (typeof $(child).data("effect-in") !== "undefined" && typeof $(child).data("effect-out") !== "undefined") {
                                $(child).removeClass($(child).data("effect-in") + " animated");
                                $(child).addClass($(child).data("effect-out") + " animated slide-out");
                            }
                        });
                    }
                });


                $slides.eq(indexOfActiveSlide).delay(700).queue(function(next) {
                    $(this).css("opacity", 0);
                    activeSlide = indexOfnewSlide;
                    $slides.eq(indexOfActiveSlide).removeAttr("style");
                    showSlide($slides.eq(indexOfnewSlide), indexOfnewSlide);
                    waitForNext();
                    next();
                });
            }

            //Whait for next slide
            function waitForNext() {
                slideTimer = setTimeout(function() {
                    changeSlides("next");
                }, config.slideDur);
            }

            /**
             * Show slides
             * @param {type} $element
             * @returns {undefined}
             */
            function showSlide($element, indexOfNewSlide) {
                //Animate progress bar
                progress(100, $(".slide" + indexOfNewSlide));
                $element.children().each(function(index, element) {

                    $(element).delay(200).queue(function(next) {
                        if (typeof $(this).data("effect-out") !== "undefined") {
                            $(this).removeClass($(this).data("effect-out") + " animated slide-out");
                        } else {
                            $(this).children().each(function(index, child) {
                                $(child).removeClass($(child).data("effect-out") + " animated slide-out");
                            });
                        }

                        $element.css({
                            "z-index": 4,
                            "opacity": 1
                        });

                        if (typeof $(this).data("effect-in") !== "undefined") {
                            $(this).stop(true, true).addClass($(this).data("effect-in") + " animated");
                        } else {
                            $(this).children().each(function(i, child) {
                                $(child).stop(true, true).addClass($(child).data("effect-in") + " animated");
                            });
                        }
                        next();
                    });
                });
            }

            // $container.find(".slide").mouseenter(function() {
            //     clearTimeout(slideTimer);
            // }).mouseleave(function() {
            //     setTimeout(function() {
            //     	    //index of next slide
            //         newSlide = activeSlide + 1;
            //         if (newSlide > totalSlides - 1) {

            //             newSlide = 0;

            //             $(".as-indicator .as-load-bar").stop().width(0);
            //         }
            //         animateSlides(activeSlide, newSlide);
            //         slideTimer;
            //             changeBG();
            //             // active the curret dot
            //             activDot();
            //     }, slideDur);

            // });


            /*Build progress bar for slides*/

            for (var i = 0; i < totalSlides; i++) {
                var htmlProgressBar = "<div class='as-indicator slide" + i + "' data-slide-number='" + i + "'><span class='as-load-bar'></span></div>";
                $(".as-indicators").append(htmlProgressBar);
            }
            // var  dotWidth = 100/totalSlides;
            if ($container.hasClass("alltogether-loadbars")) {
                $(".as-indicator").width((100 / totalSlides) - 4 + "%");
            }
            if ($container.attr("data-background-1")) {
                for (var i = 0; i < totalSlides; i++) {
                    var background = $container.attr("data-background-" + i),
                        fakeBackground = "<div class='as-background' data-slide-num='" + i + "' style='background-image: url(" + background + ")'></div>";
                    $container.append(fakeBackground);
                }
            }


            // start progress the first slide

            //Opacity for first element
            $slides.eq(0).css({
                "opacity": 1,
                "z-index": 4
            });
            activeSlide = 0;
            progress(100, $(".slide" + activeSlide));
            newSlide = 0;
            changeBG();
            activDot();
        });
    };
})(jQuery);
