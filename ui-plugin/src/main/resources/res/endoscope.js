(function($) {
    options = $.extend(true, {}, {
        valueBadLevel:  3000,
        valueWarnLevel: 1000,
        statUrl: "data/details",
        topUrl: "data/top",
        from: null,
        to: null,
        past: 3600000, //1 hour,
        sortField: 'id',
        sortDirection: 1
    }, $.localStorage.get("endoscope"));

    //debug
    window.endoscope = options;

    var placeholder;
    var loadingTim;

    function Endoscope(_placeholder) {
        placeholder = _placeholder;

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

        $(document).bind("ajaxSend", function(){
            clearTimeout(loadingTim);
            $('.es-loading').show();
        }).bind("ajaxComplete", function(){
            loadingTim = setTimeout(function(){
                $('.es-loading').hide();
            }, 500);
        });
    }

    var saveOptions = function(){
        $.localStorage.set("endoscope", options);
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
        saveOptions();
        loadTopLevel(reset);
    };

    var applyPeriodChange = function(){
        var reset = false;
        var element = $("#es-past");
        if( element.val() == -1 ){//reset and switch to custom
            options.past = null;
            options.from = new Date().getTime();
            options.to = null;
            element.val("-2");
            reset = true;
        }
        if( element.val() == -2 ){//custom
            var customOption = getCustomPeriodOption();
            var label = new Date(options.from).toISOString() + " - ";
            if( options.to != null ){
                label += new Date(options.to).toISOString();
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
        $(".es-selected").removeClass("es-selected");
    };

    var loadTopLevel = function(reset){
        clearTopLevel();
        $.ajax(options.topUrl, {
            dataType: "json",
            data: {
                from: options.from,
                to: options.to,
                past: options.past,
                reset: reset ? "true" : "false"
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

    var onTopLevelStatsLoad = function(topLevelStats) {
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
        closeAllRows();
        if( !row.hasClass('es-loading') ){
            loadChildStats(row);
        }
    };

    var removeChildStats = function(row) {
        row.nextUntil("tr.es-parent").remove();
        row.removeClass('es-expanded');
    };

    var loadChildStats = function(row) {
        row.addClass('es-loading es-selected');
        var statId = row.data('id');
        hideDetails();
        $.ajax(options.statUrl, {
            dataType: "json",
            data: {
                id: statId,
                from: options.from,
                to: options.to,
                past: options.past
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
    };

    var onDetailStatsReceive = function(details, parentRow, level) {
        processChildStats(details.merged, parentRow, level);
        buildDetails(details, parentRow);
    };

    var processChildStats = function(parentStat, parentRow, level) {
        forEachStat(parentStat.children, function(id, childStat){
            var row = $(buildRow(id, childStat, level));
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

    var buildDetails = function(details, parentRow){
        var histogram = details.histogram;
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
    };

    var extractSeries = function(histogram, property){
        var result = [];
        if( property == "bad" ){
            result.push([histogram[0].startDate, options.valueBadLevel]);
            result.push([histogram[histogram.length-1].startDate, options.valueBadLevel]);
        } else if( property == "warn" ){
            result.push([histogram[0].startDate, options.valueWarnLevel]);
            result.push([histogram[histogram.length-1].startDate, options.valueWarnLevel]);
        } else {
            histogram.forEach(function(h){
                var tick = [h.startDate, h[property]];
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
        }
        if(level == 0){
            row.attr("data-id", id);
            row.addClass("es-parent");
            row.find(".es-count").append(obj.hits);
        } else {
            row.addClass("es-child");
            row.find(".es-count").append(obj.ah10/10);
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

