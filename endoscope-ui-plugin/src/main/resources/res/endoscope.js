(function($) {
    var labels = {
        instanceLabelAll: "all instances",
        typeLabelAll: "all types"
    };

    var api = {
        statUrl: "data/details",
        histogramUrl: "data/histogram",
        topUrl: "data/top",
        filtersUrl: "data/filters"
    };

    //add host specific part so we can keep different settings for different apps
    var settingsKey = "endoscope-" + window.location.href.replace(/[^\w]/g, '');
    var options = $.extend(true, {
        valueBadLevel:  3000,
        valueWarnLevel: 1000,
        from: null,
        to: null,
        instance: null,
        type: window.endoscopeAppType,
        past: 3600000, //1 hour,
        sortField: 'id',
        sortDirection: 1
    }, $.localStorage.get(settingsKey));

    var placeholder;
    var loadingTim;
    var ajaxCount=0;
    var histogram=[];

    //https://github.com/kartik-v/php-date-formatter
    var dateFormat = 'Y/m/d H:i';
    var dateFormatter = new DateFormatter();

    function Endoscope(_placeholder) {
        placeholder = _placeholder;

        labels.instanceLabelAll = $("#es-filter").data("instance-label");
        labels.typeLabelAll  = $("#es-filter").data("type-label");

        setupAjaxLoaderImage();

        loadTopLevel();

        $("#es-refresh").click(onRefreshClick);

        setupPeriod();
        $("#es-past").change(onPeriodChange);

        $("#es-search").typeWatch({ //https://github.com/dennyferra/TypeWatch
            callback: onSearch,
            wait: 500,
            highlight: true,
            captureLength: 2
        });

        setupFilters();
        setupTimeRangeModal();
    }

    var setupAjaxLoaderImage = function(){
        $(document).bind("ajaxSend", function(){
            clearTimeout(loadingTim);
            $('.es-loading').show();
            ajaxCount++;
            //console.log("incremented ajaxCount to: " + ajaxCount);
        }).bind("ajaxComplete", function(){
            ajaxCount--;
            //console.log("decremented ajaxCount to: " + ajaxCount);
            if( ajaxCount > 0 ){ //do not hide loader if other/parallel calls are still running
                return;
            }
            //console.log("about to hide loader image");
            loadingTim = setTimeout(function(){
                $('.es-loading').hide();
            }, 500);
        });
    };

    var setupTimeRangeModal = function(){
        jQuery('#es-time-range-from').datetimepicker();
        jQuery('#es-time-range-to').datetimepicker();
        $("#es-time-range-cancel").click(onTimeRangeCancel)
        $("#es-time-range-save").click(onTimeRangeSave)
    };

    var setupFilters = function(){
        refreshFilterLabels();
        initAutocomplete($("#es-instance"), labels.instanceLabelAll);
        initAutocomplete($("#es-type"), labels.typeLabelAll);
        loadFilters();
        $("#es-filter").click(function(){
            $('#es-filter-modal').modal({
                keyboard: false
            }).on('hidden.bs.modal', function (e) {
                refreshFilterLabels();
            });
            return false;
        });
        $("#es-filter-save").click(onFiltersSave)
    };

    var onFiltersSave = function(){
        var g = $("#es-instance").val();
        options.instance = ( g == labels.instanceLabelAll ) ? null : g;
        var t = $("#es-type").val();
        options.type = ( t == labels.typeLabelAll ) ? null : t;

        saveOptions();
        loadTopLevel();
        $('#es-filter-modal').modal('hide');
    };

    var initAutocomplete = function(jqElement, value){
        jqElement.autocomplete({
            minLength: 0,
            source: [ value ],
            appendTo: "#es-filter-modal",
            autoSelect: true
        }).on("focus", function () {
            $(this).autocomplete("search", "");
            this.setSelectionRange(0, this.value.length)
        });
    };

    var prepareFilterValues = function(arr, current, none){
        arr = arr || [];

        //make sure current value is in the list
        if( current && current != none && arr.indexOf(current) == -1 ){
            arr.unshift(current);
        }

        //get rid of null and put appropriate "none" label
        var nullPos = arr.indexOf(null);
        if( nullPos >= 0 ){
            arr.splice(nullPos, 1);
        }
        arr.unshift(none);

        return arr;
    };

    var onFiltersLoad = function(filters) {
        filters.instances = prepareFilterValues(filters.instances, options.instance, labels.instanceLabelAll);
        filters.types  = prepareFilterValues(filters.types,  options.type,  labels.typeLabelAll);

        $("#es-instance").autocomplete( "option", "source", filters.instances );
        $("#es-type").autocomplete( "option", "source", filters.types );
    };

    var refreshFilterLabels = function(){
        var gl = !options.instance ? labels.instanceLabelAll : options.instance;
        var tl = !options.type ? labels.typeLabelAll : options.type;
        $("#es-instance").val(gl);
        $("#es-type").val(tl);
        $("#es-filter").text( gl + "/" + tl );
    };

    var loadFilters = function(){
        //by default keep current options
         onFiltersLoad({});

         $.ajax(api.filtersUrl, {
             dataType: "json",
             data: {
                 from: options.from,
                 to: options.to,
                 past: options.past
             }
         })
         .done($.proxy(onFiltersLoad, this))
         .fail(function(){showError("Failed to load filters")});
    };

    var saveOptions = function(){
        $.localStorage.set(settingsKey, options);
    };

    var setupPeriod = function(){//based on saved values select proper option - or reset to defaults
        if( options.past != null ){
            //if there is such option select it
            if( $("#es-past option[value=" + options.past + "]").length != 0 ){
                $("#es-past").val(options.past);
            }
        } else if( options.from != null ) {
            $("#es-past").val("-2"); //custom period
        } //else reset to default value
        applyPeriodChange();
    };

    var onPeriodChange = function(){
        var reset = applyPeriodChange();
        if( $("#es-past").val() != -3 ){ //wait for modal results
            saveOptions();
            loadTopLevel(reset);
            loadFilters();
        };
    };

    var onTimeRangeCancel = function(){
        setupPeriod(); //reset to previous value
    };

    var onTimeRangeSave = function(){
        let fromInput = $("#es-time-range-from");
        let toInput = $("#es-time-range-to");

        let fromD = dateFormatter.parseDate(fromInput.val(), dateFormat);
        let toD = dateFormatter.parseDate(toInput.val(), dateFormat);

        if( fromD == null ){
            fromInput.val('');
            return;
        }
        if( toD == null || toD <= fromD){
            toInput.val('');
            return;
        }
        options.from = fromD.getTime();
        options.to = toD.getTime();
        options.past = null;

        var element = $("#es-past");
        element.val("-2");

        onPeriodChange();
        $('#es-time-range-modal').modal('hide');
    };

    var applyPeriodChange = function(){
        var reset = false;
        var element = $("#es-past");
        if( element.val() == -3 ){
            $('#es-time-range-modal').modal({
                keyboard: false
            });
        }
        if( element.val() == -1 ){//reset and switch to custom
            options.past = null;
            options.from = new Date().getTime();
            options.to = null;
            element.val("-2");
            reset = true;
        }
        if( element.val() == -2 ){//custom
            var customOption = getCustomPeriodOption();
            var label = dateFormatter.formatDate( new Date(options.from), dateFormat);
            label += " - ";
            if( options.to != null ){
                label += dateFormatter.formatDate( new Date(options.to), dateFormat);
            } else {
                label += "now";
            }
            customOption.text(label);
            customOption.show();
        }
        if( element.val() >= 0 ){//past in ms
            options.past = element.val();
            options.from = null;
            options.to = null;
            getCustomPeriodOption().hide();
        }
        return reset;
    };

    var getCustomPeriodOption = function(){
        return $("#es-past option[value=-2]");
    };

    var onRefreshClick = function(){
        loadTopLevel();
        return false;
    };

    var onSearch = function(value){
        value = value.toLowerCase();

        closeAllRows();
        window.scrollTo(0,0);
        hideDetails();

        placeholder.find("tbody tr.es-parent").each(function(index, row){
            row = $(row);
            if( row.data().id.toLowerCase().indexOf(value) >= 0){
                row.show();
            } else {
                row.hide();
            }
        });
    };

    var closeAllRows = function(){
        placeholder.find("tbody tr.es-expanded").each(function(index, row){
            removeChildStats($(row));
        });
        $(".es-sel").removeClass("es-sel");
    };

    var loadTopLevel = function(reset){
        clearTopLevel();
        $.ajax(api.topUrl, {
            dataType: "json",
            data: {
                from: options.from,
                to: options.to,
                past: options.past,
                reset: reset ? "true" : "false",
                instance: options.instance,
                type: options.type
            }
        })
        .done($.proxy(onTopLevelStatsLoad, this))
        .fail(function(){showError("Failed to load stats data")});
    };

    var showError = function(text){
        var err = $("#es-error");
        err.find(".text").text(text);
        err.show();
    };

    var clearTopLevel = function(){
        placeholder.empty();
        hideDetails();
    };

    var hideDetails = function(){
        $(".es-details").hide();
    };

    var onTopLevelStatsLoad = function(topData) {
        var topLevelStats = topData.map;
        placeholder.empty();
        var esTable = $($("#es-table-template").html());
        placeholder.append(esTable);

        var table = esTable.find("tbody");
        var orderedStats = [];
        forEachStat(topLevelStats, function(id, stat){
            stat.id = id;
            orderedStats.push( stat );
        });
        orderedStats.sort(onSortTopLevel);
        orderedStats.forEach(function(stat){
            var row = buildRow(stat.id, stat, 0);
            table.append(row);
            row.click(onRowClick);
        });

        var headers = esTable.find("thead th");
        headers.click(onSortColumnClick);
        headers.each(function(index, th){
            th = $(th);
            var span = th.find("span");
            if(options.sortField == th.data().sort){
                if( options.sortDirection> 0 ){
                    span.addClass("glyphicon glyphicon-sort-by-attributes");
                } else {
                    span.addClass("glyphicon glyphicon-sort-by-attributes-alt");
                }
            } else {
                span.removeClass("glyphicon glyphicon-sort-by-attributes glyphicon-sort-by-attributes-alt");
            }
        });
    };

    var onSortColumnClick = function(){
        var th = $(this);
        var colId = th.data().sort;
        if( colId == options.sortField ){
            options.sortDirection *= -1;
        } else {
            options.sortField = colId;
            options.sortDirection = colId == "id" ? 1 : -1;
        }
        loadTopLevel();
        saveOptions();
    };

    var onSortTopLevel = function(a, b){
        switch(options.sortField){
            case "hits": return (a.hits - b.hits) * options.sortDirection;
            case "min":  return (a.min - b.min)   * options.sortDirection;
            case "max":  return (a.max - b.max)   * options.sortDirection;
            case "avg":  return (a.avg - b.avg)   * options.sortDirection;
            default:
                if( a.id == b.id ){
                    return 0;
                }
                return (a.id < b.id ? -1 : 1) * options.sortDirection;
        }
    };

    var onRowClick = function() {
        var row = $(this);
        var isLoading = row.hasClass('es-loading');
        var isExpanded = row.hasClass("es-sel");

        closeAllRows();
        if( isExpanded ){
            return;
        }
        if( !isLoading ){
            loadChildStats(row);
        }
    };

    var removeChildStats = function(row) {
        row.nextUntil("tr.es-parent").remove();
        row.removeClass('es-expanded');
    };

    var loadChildStats = function(row) {
        row.addClass('es-loading es-sel');
        var statId = row.data('id');

        hideDetails();//hide chart

        //load child stats
        $.ajax(api.statUrl, {
            dataType: "json",
            data: {
                id: statId,
                from: options.from,
                to: options.to,
                past: options.past,
                instance: options.instance,
                type: options.type
            }
        })
        .done(function(stats){
            row.removeClass('es-loading');
            row.addClass('es-expanded');
            onDetailStatsReceive(stats, row, 1);
        })
        .fail(function(){
            row.removeClass('es-loading');
            showError("Failed to load child stats");
        });

        //load chart
        histogram = [];
        loadHistogram(statId, null);
    };

    var loadHistogram = function(statId, lastGroupId){
        $.ajax(api.histogramUrl, {
                dataType: "json",
                data: {
                    id: statId,
                    from: options.from,
                    to: options.to,
                    past: options.past,
                    instance: options.instance,
                    type: options.type,
                    lastGroupId: lastGroupId
                }
            })
            .done(function(result){
                onHistogramReceive(result);
            })
            .fail(function(){
                showError("Failed to load histogram");
            });
    };

    var onDetailStatsReceive = function(details, parentRow, level) {
        processChildStats(details.merged, parentRow, level);
    };

    var findChildNodes = function(row){
        row = $(row);
        var level = row.data("level");

        var lteLevelSelector = "tr[data-level=0]";
        for(var l=1; l<=level; l++){
            lteLevelSelector += ",tr[data-level="+l+"]";
        }
        return row.nextUntil(lteLevelSelector);
    };

    var onExpandToggle = function(){
        var row = $(this).closest("tr");
        if( row.hasClass("es-expanded") ){
            findChildNodes(row).hide();
        } else {
            findChildNodes(row)
                .show()
                .filter('.es-has-children')
                .addClass('es-expanded');
        }
        row.toggleClass("es-expanded");
    };

    var onChildRowHoverIn = function(){
        var row = $(this).closest("tr");
        findChildNodes(row).addClass('es-highlight');
    };

    var onChildRowHoverOut = function(){
        var row = $(this).closest("tr");
        findChildNodes(row).removeClass('es-highlight');
    };

    var processChildStats = function(parentStat, parentRow, level) {
        forEachStat(parentStat.children, function(id, childStat){
            var row = $(buildRow(id, childStat, level));
            row.find(".es-btn").click(onExpandToggle);
            row.hover(onChildRowHoverIn, onChildRowHoverOut);
            parentRow.after(row);
            processChildStats(childStat, row, level+1)
        });
    };

    var forEachStat= function(stat, fn){
        if(stat == null){
            return;
        }
        for (var id in stat) {
            if (stat.hasOwnProperty(id)) {
                fn(id, stat[id]);
            }
        }
    };

    var isTimePeriodMoreThan2Days = function(){
        return options.past > 2 * 86400000;
    };

    var onHistogramReceive = function(details){
        histogram = histogram.concat(details.histogram);
        var chartOptions = {
            legend: {
                backgroundColor: null,
                show: true,
                margin: 15,
                backgroundOpacity: 0,
                labelBoxBorderColor :"#d3d3d3",
                noColumns: 2
            },
            grid: {
                borderWidth: 1,
                color: "#777777",
                hoverable: true,
                autoHighlight: false
            },
            xaxis:{
                min: details.startDate,
                max: details.endDate,
                color: "#777777",
                font: {"color": "#ffffff"},
                mode: "time",
                timeformat: isTimePeriodMoreThan2Days() ? "%m/%d" : "%H:%M"
            },
            yaxis: {
                min: 0,
                color: "#777777",
                font: {"color": "#ffffff"},
                labelWidth: 50
            }
        };

        var data, container, opts;
        data = [
            { id: "warn", data: extractSeries(histogram, "warn"), lines: { show: true, lineWidth: 1 }, color: "#f39c12" },
            { id: "bad",  data: extractSeries(histogram, "bad"),  lines: { show: true, lineWidth: 1 }, color: "#e74c3c" },
            { id: "min",  data: extractSeries(histogram, "min"),  lines: { show: true, lineWidth: 0, fill: false }, color: "#33b5e5" },
            { id: "max",  data: extractSeries(histogram, "max"),  lines: { show: true, lineWidth: 0, fill: 0.4 }, color: "#33b5e5", fillBetween: "min"},
            { label: "Average Time [ms]", id: "avg",  data: extractSeries(histogram, "avg"), lines: { show: true, lineWidth: 3 }, color: "#33b5e5" }
        ];
        container = $(".es-details .es-chart-times");
        container.empty();
        $(".es-details .es-title").text(details.id);
        $(".es-details").show();
        opts = $.extend(true, {}, chartOptions, {yaxis: {tickFormatter: function (x) {return x + " ms";}}});
        $.plot(container, data, opts);

        data = [
            { label: "Hits per second", id: "hits", data: extractSeries(histogram, "hits"), lines: { show: true, steps: true, lineWidth: 3, fill: 0.7 }, color: "#5cb85c"}
        ];
        container = $(".es-details .es-chart-hits");
        container.empty();
        opts = $.extend(true, {}, chartOptions, {yaxis: {tickDecimals: 2}});
        $.plot(container, data, opts);

        //TODO plot details
        // require grid.hoverable: true
        //$(container).bind( "plothover", function ( evt, position, item ) {
        //    console.log(JSON.stringify({pos: position, item: item}));
        //});

        //load next parts if exists
        if( details.lastGroupId != null ){
            loadHistogram(details.id, details.lastGroupId);
        }
    };

    var extractSeries = function(histogram, property){
        var result = [];
        if( histogram == null || histogram.length == 0 ){
            //do nothing
        } else if( property == "bad" ){
            result.push([histogram[0].startDate, options.valueBadLevel]);
            result.push([histogram[histogram.length-1].endDate, options.valueBadLevel]);
        } else if( property == "warn" ){
            result.push([histogram[0].startDate, options.valueWarnLevel]);
            result.push([histogram[histogram.length-1].endDate, options.valueWarnLevel]);
        } else {
            histogram.forEach(function(h){
                var time = (h.startDate + h.endDate)/2;
                var tick = [time, h[property]];
                if( property == "hits" ){
                    //convert to average tick per second, as total hits doesn't look well espiecially when tick length may differ
                    var seconds = (h.endDate - h.startDate)/1000;
                    tick[1] = tick[1]/seconds;
                }
                result.push(tick);
            });
        }
        return result;
    };

    var buildRow = function(id, obj, level){
        var row = $($("#es-row-template").html());

        if( obj.children ){
            row.addClass("es-has-children");
            //autoexpand all children
            row.addClass("es-expanded");
        }
        row.attr("data-level", level);
        if(level == 0){
            row.attr("data-id", id);
            row.addClass("es-parent");
            row.find(".es-count").append(obj.hits);
        } else {
            row.addClass("es-child es-sel");
            row.find(".es-count").append(obj.ah10/10);
            if( level > 2 ){
                row.hide();//no autoexpand at higher levels
            }
        }
        row.attr("title", id);

        row.find(".es-id").append(indent(level)).append(id);
        addNumberValue( row.find(".es-max"), obj.max);
        addNumberValue( row.find(".es-min"), obj.min);
        addNumberValue( row.find(".es-avg"), obj.avg);

        return row;
    };

    var addNumberValue = function(el, val){
        var tpl = valueTemplate(val);
        if( tpl ){
            val = $($(tpl).html()).text(val);
        }
        el.append(val);
    };

    var valueTemplate = function(time){
        if( time > options.valueBadLevel ){
            return "#es-bad-template"
        }
        if( time > options.valueWarnLevel ){
            return "#es-warn-template"
        }
        return null;
    };

    var indent = function(count){
        var indentHtml = $("#es-indent-template").html();
        var result = '';
        for(var i=0; i<count; i++){
            result += indentHtml;
        }
        return $(result);
    };

    $.endoscope = function(placeholder){
        return new Endoscope($(placeholder));
    }
})(jQuery);

