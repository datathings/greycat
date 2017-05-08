$(".input-field").each(function () {
    var $this = $(this);
    if ($this.val().length) {
        $this.parent().addClass("input--filled")
    }
    $this.on("focus", function () {
        $this.parent().addClass("input--filled");
    });
    $this.on("blur", function () {
        if (!$this.val().length) {
            $this.parent().removeClass("input--filled")
        }
    })
});



$(function () {


    // Get the form.
    var form = $('#ajax-contact'),
        // Get the messages div.
        formMessages = $('#form-messages');

    // Set up an event listener for the contact form.
    $(form).submit(function (e) {
        // Stop the browser from submitting the form.
        e.preventDefault();


        $("#btn-submit").addClass("btn-loading");


        // Serialize the form data.
        var formData = $(form).serialize();

        // Submit the form using AJAX.
        $.ajax({
                type: 'POST',
                url: $(form).attr('action'),
                data: formData
            })
            .done(function (response) {
                // Make sure that the formMessages div has the 'success' class.
                $(formMessages).removeClass('error').addClass('success').fadeIn().delay(5000).fadeOut();
                // Set the message text.
                $(formMessages).text(response);

                // Clear the form.
                $(form).trigger("reset");
                $("#btn-submit").removeClass("btn-loading");
            })
            .fail(function (data) {
                // Make sure that the formMessages div has the 'error' class.
                $(formMessages).removeClass('success').addClass('error').fadeIn().delay(5000).fadeOut();
                // Set the message text.
                if (data.responseText !== '') {
                    $(formMessages).text(data.responseText);
                } else {
                    $(formMessages).text('Oops! An error occured and your message could not be sent.');
                }


                $("#btn-submit").removeClass("btn-loading");
            });

    });

});


$(function () {


    // Get the form.
    var form = $('#newsletter-form'),
        // Get the messages div.
        formMessages = $('#newsletter-messages');

    // Set up an event listener for the contact form.
    $(form).submit(function (e) {
        // Stop the browser from submitting the form.
        e.preventDefault();


        $("#btn-newsletter").addClass("btn-loading");


        // Serialize the form data.
        var formData = $(form).serialize();

        // Submit the form using AJAX.
        $.ajax({
                type: 'POST',
                url: $(form).attr('action'),
                data: formData
            })
            .done(function (response) {
                // Make sure that the formMessages div has the 'success' class.
                $(formMessages).removeClass('error');
                $(formMessages).addClass('success');
                setTimeout(function () {
                    $(formMessages).addClass("fade-outy");
                }, 5000);

                // Set the message text.
                $(formMessages).text(response);

                // Clear the form.
                $(form).trigger("reset");
                $("#btn-newsletter").removeClass("btn-loading");
            })
            .fail(function (data) {
                // Make sure that the formMessages div has the 'error' class.
                $(formMessages).removeClass('success');
                $(formMessages).addClass('error');
                setTimeout(function () {
                    $(formMessages).addClass("fade-outy");
                }, 5000);

                // Set the message text.
                if (data.responseText !== '') {
                    $(formMessages).text(data.responseText);
                } else {
                    $(formMessages).text('Oops! An error occured and your message could not be sent.');
                }


                $("#btn-newsletter").removeClass("btn-loading");
            });

    });

});
