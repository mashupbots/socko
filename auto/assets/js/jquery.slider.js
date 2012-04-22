(function($) {
	$.fn.slider = function(options) {
		// Merge the user specified options with the default options
		var o = $.extend({
			slideSelector: '.slide',
			waitTime: 5000
		}, options);
		
		$(this).each(function() {
			// Set up some variables
			var $this = $(this);
			
			var slides = $this.find(o.slideSelector);
			var currentSlide = 0;
			var $slideContainer = $this.find('#slide_container');
			var timeoutId = -1;
			
			// Add nav buttons for each slide 
			$.each(slides, function(index, value) {
				var cls = (index == 0) ? 'active' : '';
				$this.find('#slider_navigation').append('<a class="' + cls + '" href="#"></a>');
			});
			
			var $slideButtons = $this.find('#slider_navigation a');
			
			// This function animates the slides to slide left or right
			function animateSlide() {
				$slideButtons.removeClass('active');
				$($slideButtons[currentSlide]).addClass('active');

				if ($.browser.msie) {
					$slideContainer.animate({
						left: String(-(currentSlide * $(slides[currentSlide]).outerWidth(true))) + 'px'
					}, 300);
				} else {
					$slideContainer.css('left', String(-(currentSlide * $(slides[currentSlide]).outerWidth(true))) + 'px');
				}
			}
		
			// This function slides the slider to the specified slideIndex
			function slideTo(slideIndex) {
				clearTimeout(timeoutId);
				
				if (slideIndex == slides.length) {
					slideIndex = 0;
				} else if (slideIndex < 0) {
					slideIndex = slides.length - 1;
				}
				
				currentSlide = slideIndex;
				
				animateSlide();
				
				timeoutId = setTimeout(doSlide, o.waitTime);
			}
			
			// This function slides the slider to the next slide, or back to the beginning
			function doSlide() {
				++currentSlide;
				
				if (currentSlide == slides.length) {
					currentSlide = 0;
				}
				
				animateSlide();
				
				timeoutId = setTimeout(doSlide, o.waitTime);
			}
			
			$slideButtons.click(function(event) {
				slideTo($.inArray(this, $slideButtons));
				
				event.preventDefault();
			});
			
			$this.find('.arrow').click(function(event) {
				var direction = ($(this).attr('rel') == 'back') ? -1 : 1;
				
				slideTo(currentSlide + direction);
				
				event.preventDefault();
			});
			
			$this.mousemove(function(event) {
				if (event.pageX < 585) {
					$this.find('#right_arrow').stop(true, true).fadeOut(300);
					$this.find('#left_arrow').stop(true, true).fadeIn(300);
				} else {
					$this.find('#left_arrow').stop(true, true).fadeOut(300);
					$this.find('#right_arrow').stop(true, true).fadeIn(300);
				}
			}).mouseleave(function() {
				$this.find('.arrow').hide();
			});
			
			timeoutId = setTimeout(doSlide, o.waitTime);
		});
	};
})(jQuery);
