/**
 * AgendaWeekSkin.java
 *
 * Copyright (c) 2011-2014, JFXtras
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the organization nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jfxtras.internal.scene.control.skin.agenda;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SkinBase;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import jfxtras.animation.Timer;
import jfxtras.internal.scene.control.skin.DateTimeToCalendarHelper;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.util.NodeUtil;

/**
 * @author Tom Eugelink
 */
// TBEERNOT: TODO: whole day Task (TaskHeaderPane)
public class AgendaWeekSkin extends SkinBase<Agenda>
implements AgendaSkin
{
	// ==================================================================================================================
	// CONSTRUCTOR
	
	/**
	 * 
	 */
	public AgendaWeekSkin(Agenda control)
	{
		super(control);
		construct();
	}

	/*
	 * construct the component
	 */
	private void construct()
	{	
		appointments = new AllAppointments(getSkinnable().appointments());
		
		// setup component
		createNodes();

		// react to changes in the locale 
		getSkinnable().localeProperty().addListener( (observable) -> {
			refreshLocale();
		});
		refreshLocale();
		
		// react to changes in the displayed calendar 
		getSkinnable().displayedDateTime().addListener( (observable) -> {
			assignDateToDayAndHeaderPanes();
			setupAppointments();
		});
		assignDateToDayAndHeaderPanes();
		
		// react to changes in the appointments 
		getSkinnable().appointments().addListener( (javafx.collections.ListChangeListener.Change<? extends Appointment> change) -> {
			setupAppointments();
		});
		setupAppointments();
		
		// react to changes in the appointments 
		getSkinnable().selectedAppointments().addListener( (javafx.collections.ListChangeListener.Change<? extends Appointment> change) -> {
			setOrRemoveSelected();
		});
		setOrRemoveSelected();
	}
	AllAppointments appointments = null;
	
	/**
	 * set or remove the Selected class from the appointments
	 */
	// TBEERNOT: can we move this to a class shared by week and day skin?
	private void setOrRemoveSelected()
	{
		// update the styleclass
		for (DayBodyPane lDayPane : weekBodyPane.dayBodyPanes)
		{
			for (AppointmentAbstractPane lAppointmentPane : lDayPane.trackedAppointmentBodyPanes)
			{
				// remove 
				if ( lAppointmentPane.getStyleClass().contains("Selected") == true
				  && getSkinnable().selectedAppointments().contains(lAppointmentPane.appointment) == false
				   )
				{
					lAppointmentPane.getStyleClass().remove("Selected");
				}
				// add
				if ( lAppointmentPane.getStyleClass().contains("Selected") == false
				  && getSkinnable().selectedAppointments().contains(lAppointmentPane.appointment) == true
				   )
				{
					lAppointmentPane.getStyleClass().add("Selected");
				}
			}
		}		
	}
	
	/**
	 * Assign a calendar to each day, so it knows what it must draw.
	 */
	private void assignDateToDayAndHeaderPanes()
	{
		// get the first day of week
		LocalDate lStartLocalDate = getFirstDayOfWeekLocalDate();
		LocalDate lLocalDate = lStartLocalDate;
		
		// assign it to each day pane
		for (int i = 0; i < 7; i++)
		{
			// set the calendar
			DayBodyPane lDayPane = weekBodyPane.dayBodyPanes.get(i); 
			lDayPane.localDateObjectProperty.set(lLocalDate);
			DayHeaderPane lDayHeaderPane = weekHeaderPane.dayHeaderPanes.get(i); 
			lDayHeaderPane.localDateObjectProperty.set(lLocalDate);
			lLocalDate = lLocalDate.plusDays(1);
		}		
		LocalDate lEndLocalDate = lLocalDate;
		
		// place the now line
		nowUpdateRunnable.run(); 
		
		// tell the control what range is displayed, so it can update the appointments
		if (getSkinnable().getDateTimeRangeCallback() != null) {
			Agenda.DateTimeRange lRange = new Agenda.DateTimeRange(lStartLocalDate.atStartOfDay(), lEndLocalDate.plusDays(1).atStartOfDay());
			getSkinnable().getDateTimeRangeCallback().call(lRange);
		}
		if (getSkinnable().getCalendarRangeCallback() != null) {
			Agenda.CalendarRange lRange = new Agenda.CalendarRange( DateTimeToCalendarHelper.createCalendarFromLocalDate(lStartLocalDate, Locale.getDefault()), DateTimeToCalendarHelper.createCalendarFromLocalDate(lEndLocalDate, Locale.getDefault()));
			getSkinnable().getCalendarRangeCallback().call(lRange);
		}
	}
	
	/**
	 * 
	 */
	private void refreshLocale()
	{
		// create the formatter to use
		layoutHelp.dayOfWeekDateFormat = new SimpleDateFormat("E", getSkinnable().getLocale());
		layoutHelp.dayOfWeekDateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("E").toFormatter(getSkinnable().getLocale());
		layoutHelp.dateFormat = (SimpleDateFormat)SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT, getSkinnable().getLocale());
		layoutHelp.dateDateTimeFormatter= new DateTimeFormatterBuilder().appendLocalized(FormatStyle.SHORT, null).toFormatter(getSkinnable().getLocale());
		
		// TBEERNOT: force redraw the dayHeaders upon local change 
		for (DayBodyPane lDayBodyPane : weekBodyPane.dayBodyPanes)
		{
			// set weekend class
			String lWeekendOrWeekday = isWeekend(lDayBodyPane.localDateObjectProperty.get()) ? "weekend" : "weekday";
			lDayBodyPane.getStyleClass().removeAll("weekend", "weekday");
			lDayBodyPane.getStyleClass().add(lWeekendOrWeekday);			
		}
		for (DayHeaderPane lDayHeaderPane : weekHeaderPane.dayHeaderPanes) {
			String lWeekendOrWeekday = isWeekend(lDayHeaderPane.localDateObjectProperty.get()) ? "weekend" : "weekday";
			lDayHeaderPane.getStyleClass().removeAll("weekend", "weekday");
			lDayHeaderPane.getStyleClass().add(lWeekendOrWeekday);			
		}
	}

	/**
	 * Have all days reconstruct the appointments
	 */
	public void setupAppointments() {
		calculateSizes();
		for (DayHeaderPane lDay : weekHeaderPane.dayHeaderPanes) {
			lDay.setupAppointments();
		}
		for (DayBodyPane lDay : weekBodyPane.dayBodyPanes) {
			lDay.setupAppointments();
		}
		calculateSizes(); // TBEERNOT: when dropping a wholeday appointment into another day header, the header height is not increased unless this call is present. Figure out why.
		nowUpdateRunnable.run(); // set the history
	}

	/**
	 * 
	 */
	public void refresh() {
		refreshLocale();
		assignDateToDayAndHeaderPanes();
		setupAppointments();
		setOrRemoveSelected();
		nowUpdateRunnable.run(); 
	}
	
	// ==================================================================================================================
	// DRAW
	
	/**
	 * construct the nodes
	 */
	private void createNodes()
	{
		// we use a borderpane
		borderPane = new BorderPane();
		borderPane.prefWidthProperty().bind(getSkinnable().widthProperty()); // the border pane is the same size as the whole skin
		borderPane.prefHeightProperty().bind(getSkinnable().heightProperty());
		getChildren().add(borderPane);
		
		// borderpane center
		weekBodyPane = new WeekBodyPane();
		weekScrollPane = new ScrollPane();
		weekScrollPane.setContent(weekBodyPane);
		weekScrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		weekScrollPane.setFitToWidth(true);
		weekScrollPane.setPannable(false); // panning would conflict with creating a new appointment
		borderPane.setCenter(weekScrollPane);
		// bind to the scrollpane's viewport
		weekScrollPane.viewportBoundsProperty().addListener( (observable) -> {
			calculateSizes();
			nowUpdateRunnable.run();
		});
		
		// borderpane top: header has to be created after the content, because there is a binding
		weekHeaderPane = new WeekHeaderPane(weekBodyPane);
		weekHeaderPane.prefWidthProperty().bind(weekBodyPane.widthProperty()); // same width as the weekpane
		weekHeaderPane.prefHeightProperty().bind(layoutHelp.headerHeightProperty);
		weekHeaderPane.setTranslateX(1); // correct for the scrollpane
		borderPane.setTop(weekHeaderPane);
		
		// the borderpane is placed in the drag pane, so DragPane can catch mouse events
		getChildren().remove(borderPane);
		layoutHelp.dragPane.getChildren().add(borderPane);
		getChildren().add(layoutHelp.dragPane);
		
		// style
		getSkinnable().getStyleClass().add(getClass().getSimpleName()); // always add self as style class, because CSS should relate to the skin not the control		
	}
	private BorderPane borderPane = null;
	private WeekHeaderPane weekHeaderPane = null;
	private ScrollPane weekScrollPane = null;
	private WeekBodyPane weekBodyPane = null;

	// ==================================================================================================================
	// PANES
	
	/**
	 * Responsible for rendering the day headers within the week
	 */
	class WeekHeaderPane extends Pane
	{
		/**
		 * 
		 */
		public WeekHeaderPane(final WeekBodyPane weekPane)
		{
			// one day header pane per day body pane 
			for (DayBodyPane dayBodyPane : weekPane.dayBodyPanes)
			{
				// create pane
				DayHeaderPane lDayHeader = new DayHeaderPane(dayBodyPane.localDateObjectProperty.get(), appointments, layoutHelp); // associate with a day, so we can use its administration. This needs only be done once
				
				// layout in relation to day panes
				lDayHeader.layoutXProperty().bind(dayBodyPane.layoutXProperty()); // same x position as the body			
				lDayHeader.layoutYProperty().set(0);
				lDayHeader.prefWidthProperty().bind(dayBodyPane.prefWidthProperty()); // same width as the body			
				lDayHeader.prefHeightProperty().bind(heightProperty()); // same heigh as the week pane
				getChildren().add(lDayHeader);
				
				// remember
				dayHeaderPanes.add(lDayHeader);
			}
		}
		final List<DayHeaderPane> dayHeaderPanes = new ArrayList<DayHeaderPane>();
	}

	/**
	 * Responsible for rendering the days within the week
	 */
	class WeekBodyPane extends Pane
	{
		final List<DayBodyPane> dayBodyPanes = new ArrayList<DayBodyPane>();

		/**
		 * 
		 */
		public WeekBodyPane()
		{
			getStyleClass().add("Week");
			getChildren().add(new TimeScale24Hour(this, layoutHelp));

			// 7 days per week
			LocalDate localDate = getFirstDayOfWeekLocalDate();
			for (int i = 0; i < 7; i++)
			{
				DayBodyPane lDayPane = new DayBodyPane(localDate, appointments, layoutHelp);
				lDayPane.setId("dayPane" + i);
				lDayPane.layoutXProperty().bind(layoutHelp.dayWidthProperty.multiply(i).add(layoutHelp.dayFirstColumnXProperty));
				lDayPane.layoutYProperty().set(0.0);
				lDayPane.prefWidthProperty().bind(layoutHelp.dayWidthProperty);
				lDayPane.prefHeightProperty().bind(layoutHelp.dayHeightProperty);
				getChildren().add(lDayPane);
				
				// remember
				dayBodyPanes.add(lDayPane);
				localDate = localDate.plusDays(1);
			}
		}
	}
	
	// ==================================================================================================================
	// NOW
	
	final Rectangle nowLine = new Rectangle(0,0,0,0);
	
	/**
	 * This is implemented as a runnable so it can be called from a timer, but also directly
	 */
	Runnable nowUpdateRunnable = new Runnable()
	{
		{
			nowLine.getStyleClass().add("Now");
			nowLine.setHeight(3);
		}
		
		@Override
		public void run()
		{
			//  get now
			LocalDateTime lNow = LocalDateTime.now();
			LocalDate lToday = lNow.toLocalDate();
			
			// see if we are displaying now (this has to do with the fact that now may slide in or out of the view)
			// check all days
			boolean lFound = false;
			for (DayBodyPane lDayPane : weekBodyPane.dayBodyPanes) {
				
				// if the date of the day is the same day as now
				if (lDayPane.localDateObjectProperty.get().isEqual(lToday) == false) {
					
					// not today
					lDayPane.getStyleClass().remove("today");					
				}
				else {
					
					// today
					if (lDayPane.getStyleClass().contains("today") == false) {
						lDayPane.getStyleClass().add("today");
					}
					lFound = true;
					
					// add if not present
					if (weekBodyPane.getChildren().contains(nowLine) == false) {
						weekBodyPane.getChildren().add(nowLine); // this will remove the now line from another day 
						nowLine.xProperty().bind(lDayPane.layoutXProperty());
					}

					// place it
					int lOffsetY = (lNow.getHour() * 60) + lNow.getMinute();
					nowLine.setY(NodeUtil.snapXY(layoutHelp.dayHeightProperty.get() / (24 * 60) * lOffsetY) );
					if (nowLine.widthProperty().isBound() == false) {
						nowLine.widthProperty().bind(layoutHelp.dayWidthProperty);	
					}
				}
			}
			
			// if cannot be placed, remove
			if (lFound == false) {
				weekBodyPane.getChildren().remove(nowLine);
			}
			
			// history 
			for (DayHeaderPane lDayHeaderPane : weekHeaderPane.dayHeaderPanes) {
				for (Node lNode : lDayHeaderPane.getChildren()) {
					if (lNode instanceof AppointmentAbstractPane) {
						((AppointmentAbstractPane)lNode).determineHistoryVisualizer(lNow);
					}
				}
			}
			for (DayBodyPane lDayBodyPane : weekBodyPane.dayBodyPanes) {
				for (Node lNode : lDayBodyPane.getChildren()) {
					if (lNode instanceof AppointmentAbstractPane) {
						((AppointmentAbstractPane)lNode).determineHistoryVisualizer(lNow);
					}
				}
			}
		}
	};
	
	/**
	 * This timer takes care of updating NOW
	 */
	Timer nowTimer = new Timer(nowUpdateRunnable)
		.withCycleDuration(new Duration(60 * 1000)) // every minute
		.withDelay(new Duration( ((60 - LocalDateTime.now().getSecond()) * 1000) - (LocalDateTime.now().getNano() / 1000000)) ) // trigger exactly on each new minute
		.start();  

	
	// ==================================================================================================================
	// SUPPORT

	/**
	 * check if a certain weekday name is a certain day-of-the-week
	 */
	private boolean isWeekend(LocalDate localDate) 
	{
		return (localDate.getDayOfWeek() == DayOfWeek.SATURDAY) || (localDate.getDayOfWeek() == DayOfWeek.SUNDAY);
	}
	

	/**
	 * 
	 */
	private void calculateSizes()
	{
		// TBEERNOT: todo: move bind into LayoutHelp
		// TBEERNOT: todo: less set, more binds
		
		// generic
		double lScrollbarSize = new ScrollBar().getWidth();
		layoutHelp.textHeightProperty.set( new Text("X").getBoundsInParent().getHeight() );
		
		// header
		AllAppointments lAllAppointments = new AllAppointments(getSkinnable().appointments());
		int maxOfWholeDayAppointments = 0;
		for (DayBodyPane lDay : weekBodyPane.dayBodyPanes)
		{
			int numberOfWholeDayAppointments = lAllAppointments.collectWholedayFor(lDay.localDateObjectProperty.get()).size();
			maxOfWholeDayAppointments = Math.max(maxOfWholeDayAppointments, numberOfWholeDayAppointments);
		}
		layoutHelp.highestNumberOfWholedayAppointmentsProperty.set(maxOfWholeDayAppointments);
		layoutHelp.titleDateTimeHeightProperty.bind( layoutHelp.textHeightProperty.multiply(1.5) ); 
		layoutHelp.appointmentHeaderPaneHeightProperty.bind( layoutHelp.textHeightProperty.add(5) ); // not sure why the 5 is needed
		layoutHelp.headerHeightProperty.bind( layoutHelp.highestNumberOfWholedayAppointmentsProperty.multiply(layoutHelp.appointmentHeaderPaneHeightProperty).add(layoutHelp.titleDateTimeHeightProperty) );

		// time column
		layoutHelp.timeWidthProperty.set( new Text("88:88").getBoundsInParent().getWidth() + layoutHelp.timeColumnWhitespaceProperty.get() );
		
		// day columns
		layoutHelp.dayFirstColumnXProperty.bind( layoutHelp.timeWidthProperty );
		if (weekScrollPane.viewportBoundsProperty().get() != null) 
		{
			layoutHelp.dayWidthProperty.set( (weekScrollPane.viewportBoundsProperty().get().getWidth() - layoutHelp.timeWidthProperty.get()) / 7 ); // 7 days per week
		}
		layoutHelp.dayContentWidthProperty.bind( layoutHelp.dayWidthProperty.subtract(10) ); // the 10 is a margin at the right so that there is always room to start a new appointment
		
		// hour height
//		layoutHelp.hourHeighProperty.bind( layoutHelp.textHeightProperty.multiply(2).add(10) ); // 10 is padding
		layoutHelp.hourHeighProperty.set( (layoutHelp.textHeightProperty.get() * 2) + 10 ); // 10 is padding
		if (weekScrollPane.viewportBoundsProperty().get() != null && (weekScrollPane.viewportBoundsProperty().get().getHeight() - lScrollbarSize) > layoutHelp.hourHeighProperty.get() * 24)
		{
			// if there is more room than absolutely required, let the height grow with the available room
			layoutHelp.hourHeighProperty.set( (weekScrollPane.viewportBoundsProperty().get().getHeight() - lScrollbarSize) / 24 );
		}
		layoutHelp.dayHeightProperty.bind(layoutHelp.hourHeighProperty.multiply(24));
		DoubleProperty msPerDayProperty = new SimpleDoubleProperty(24 * 60 * 60 * 1000);
		layoutHelp.durationInMSPerPixelProperty.bind( msPerDayProperty.divide(layoutHelp.dayHeightProperty) );
	}
	private LayoutHelp layoutHelp = new LayoutHelp(getSkinnable(), this);
	
	
	/**
	 * get the datetime for the first day of the week
	 */
	private LocalDate getFirstDayOfWeekLocalDate()
	{
		Locale lLocale = getSkinnable().getLocale();
		WeekFields lWeekFields = WeekFields.of(lLocale);
		int lFirstDayOfWeek = lWeekFields.getFirstDayOfWeek().getValue();
		LocalDate lDisplayedDateTime = getSkinnable().getDisplayedDateTime().toLocalDate();
		int lCurrentDayOfWeek = lDisplayedDateTime.getDayOfWeek().getValue();

		// result
		if (lFirstDayOfWeek <= lCurrentDayOfWeek)
		{
			lDisplayedDateTime = lDisplayedDateTime.plusDays(-lCurrentDayOfWeek + lFirstDayOfWeek);
		}
		else
		{
			lDisplayedDateTime = lDisplayedDateTime.plusDays(-lCurrentDayOfWeek - (7-lFirstDayOfWeek));
		}
		return lDisplayedDateTime;
	}
	

	/**
	 * 
	 */
	public LocalDateTime convertClickToDateTime(double x, double y) {
		
		// the click has only value in either the day panes 
		for (DayBodyPane lDayPane : weekBodyPane.dayBodyPanes) {
			LocalDateTime localDateTime = lDayPane.convertClickToDateTime(x, y);
			if (localDateTime != null) {
				return localDateTime;
			}
		}
		// or the day header panes
		for (DayHeaderPane lDayHeaderPane : weekHeaderPane.dayHeaderPanes) {
			LocalDateTime localDateTime = lDayHeaderPane.convertClickToDateTime(x, y);
			if (localDateTime != null) {
				return localDateTime;
			}
		}
		return null;
	}
}
