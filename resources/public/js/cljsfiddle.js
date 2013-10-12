(function () {

    CodeMirror.defineOption("cljsfiddleButtons", false, function(cm, opt, obj) {
	if (!opt) return;
	var runButton = '<button class="btn btn-default btn-xs" id="run-btn" data-toggle="tooltip" title="Compile & Run"><span class="glyphicon glyphicon-play"></span></button>';
	var saveButton = '<button class="btn btn-default btn-xs" id="save-btn" data-toggle="tooltip" title="Save"><span class="glyphicon glyphicon-floppy-save"></span></button>';
	var spanFragment = '<span id="cljsfiddle-cm-buttons">' 
	                   + runButton 
	                   + saveButton
	                   + '</span>';
	var wrapper = cm.getWrapperElement();
	$(wrapper).prepend(spanFragment);
    });
})();
