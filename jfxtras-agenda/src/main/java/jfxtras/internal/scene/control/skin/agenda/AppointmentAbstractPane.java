package jfxtras.internal.scene.control.skin.agenda;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;

import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import jfxtras.scene.control.agenda.Agenda;
import jfxtras.util.NodeUtil;

abstract public class AppointmentAbstractPane extends Pane {
	/**
	 * @param calendar
	 * @param appointment
	 */
	public AppointmentAbstractPane(Agenda.Appointment appointment, LayoutHelp layoutHelp, Draggable draggable)
	{
		this.appointment = appointment;
		this.layoutHelp = layoutHelp;
		this.draggable = draggable;

		// for debugging setStyle("-fx-border-color:PINK;-fx-border-width:1px;");
		getStyleClass().add("Appointment");
		getStyleClass().add(appointment.getAppointmentGroup().getStyleClass());
		
		// historical visualizer
		historyVisualizer = new HistoricalVisualizer(this);
		getChildren().add(historyVisualizer);

		// tooltip
		if (appointment.getSummary() != null) {
			Tooltip.install(this, new Tooltip(appointment.getSummary()));
		}
		
		setupDragging();
	}
	final protected Agenda.Appointment appointment; 
	final protected LayoutHelp layoutHelp;
	final protected Draggable draggable;
	enum Draggable { YES, NO }
	final protected HistoricalVisualizer historyVisualizer;

	/**
	 * 
	 * @param now
	 */
	public void determineHistoryVisualizer(LocalDateTime now) {
		historyVisualizer.setVisible(appointment.getStartDateTime().isBefore(now));
	}

	/**
	 * 
	 */
	private void setupDragging() {
		// ------------
		// dragging
		
		// start drag
		setOnMousePressed( (mouseEvent) -> {					
			// no one else
			mouseEvent.consume();
			if (mouseEvent.isPrimaryButtonDown() == false) {
				return;
			}
			if (draggable != Draggable.YES) {
				return;
			}

			// no drag yet
			mouseActuallyHasDragged = mouseEvent.isPrimaryButtonDown() ? false : true; // if not primary mouse, then just assume drag from the start 

			// place the rectangle
			setCursor(Cursor.MOVE);
			double lX = NodeUtil.screenX(this) - NodeUtil.screenX(layoutHelp.dragPane);
			double lY = NodeUtil.screenY(this) - NodeUtil.screenY(layoutHelp.dragPane);
			dragRectangle = new Rectangle(NodeUtil.snapXY(lX), NodeUtil.snapXY(lY), NodeUtil.snapWH(lX, getWidth()), NodeUtil.snapWH(lY, (appointment.isWholeDay() ? layoutHelp.titleDateTimeHeightProperty.get() : getHeight())) );
			dragRectangle.getStyleClass().add("GhostRectangle");
			layoutHelp.dragPane.getChildren().add(dragRectangle);
			
			// remember
			startX = mouseEvent.getScreenX();
			startY = mouseEvent.getScreenY();
			
			// TBEERNOT: determine start date and time of drag:
			// - date = ask extender via abstract method or via constructor
			// - time = calculate by offset from start of pane, ask extender
		});
		
		// visualize dragging
		setOnMouseDragged((mouseEvent) -> {
			// no one else
			mouseEvent.consume();
			if (mouseEvent.isPrimaryButtonDown() == false) return;

			// no dragged
			mouseActuallyHasDragged = true;
			if (dragRectangle == null) return;
			
			double lDeltaX = mouseEvent.getScreenX() - startX;
			double lDeltaY = mouseEvent.getScreenY() - startY;
			double lX = NodeUtil.screenX(this) - NodeUtil.screenX(layoutHelp.dragPane) + lDeltaX;
			double lY = NodeUtil.screenY(this) - NodeUtil.screenY(layoutHelp.dragPane) + lDeltaY;
			dragRectangle.setX(NodeUtil.snapXY(lX));
			dragRectangle.setY(NodeUtil.snapXY(lY));
			
			// no one else
			mouseEvent.consume();
		});
		
		// end drag
		setOnMouseReleased((mouseEvent) -> {
			// no one else
			mouseEvent.consume();

			// reset ui
			boolean lDragRectangleWasVisible = (dragRectangle != null);
			setCursor(Cursor.HAND);
			layoutHelp.dragPane.getChildren().remove(dragRectangle);
			dragRectangle = null;					
			
			// -----
			// select
			
			// if have not dragged (even if the drag rectangle was shown), then we're selecting
			if (mouseActuallyHasDragged == false) {
				
				// if not shift pressed, clear the selection
				if (mouseEvent.isShiftDown() == false && mouseEvent.isControlDown() == false) {
					layoutHelp.skinnable.selectedAppointments().clear();
				}
				
				// add to selection if not already added
				if (layoutHelp.skinnable.selectedAppointments().contains(appointment) == false) {
					layoutHelp.skinnable.selectedAppointments().add(appointment);
				}
				// pressing control allows to toggle
				else if (mouseEvent.isControlDown()) {
					layoutHelp.skinnable.selectedAppointments().remove(appointment);
				}
				return;
			}
			
			// ------------
			// dragging
			
			if (lDragRectangleWasVisible == false) {
				return;
			}
			
			// determine startDateTime of the drag
			LocalDateTime dragStartDateTime = layoutHelp.skin.convertClickToDateTime(startX, startY);
			boolean dragStartInDayBody = dragInDayBody(dragStartDateTime);
			boolean dragStartInDayHeader = dragInDayHeader(dragStartDateTime);
			dragStartDateTime = layoutHelp.roundTimeToNearestMinutes(dragStartDateTime, roundToMinutes);
			
			// determine endDateTime of the drag
			LocalDateTime dragEndDateTime = layoutHelp.skin.convertClickToDateTime(mouseEvent.getScreenX(), mouseEvent.getScreenY());
			if (dragEndDateTime == null) {
				// dropped somewhere outside, abort
				return;
			}
			boolean dragEndInDayBody = dragInDayBody(dragEndDateTime);
			boolean dragEndInDayHeader = dragInDayHeader(dragEndDateTime);
			dragEndDateTime = layoutHelp.roundTimeToNearestMinutes(dragEndDateTime, roundToMinutes);

			// TBEERNOT: show time label in dragged rectangle
			// TBEERNOT: move code out to the DragPane
			
			// if dragged from day to day or header to header
			if ( (dragStartInDayBody && dragEndInDayBody) 
			  || (dragStartInDayHeader && dragEndInDayHeader)
			) {				
				// simply add the duration
				Duration duration = Duration.between(dragStartDateTime, dragEndDateTime);
				if (appointment.getStartDateTime() != null) {
					appointment.setStartDateTime( appointment.getStartDateTime().plus(duration) );
				}
				if (appointment.getEndDateTime() != null) {
					appointment.setEndDateTime( appointment.getEndDateTime().plus(duration) );
				}
			}
			
			// if dragged from day to header
			else if ( (dragStartInDayBody && dragEndInDayHeader) ) {
				
				appointment.setWholeDay(true);
				
				// simply add the duration, but without time
				Period period = Period.between(dragStartDateTime.toLocalDate(), dragEndDateTime.toLocalDate());
				if (appointment.getStartDateTime() != null) {
					appointment.setStartDateTime( appointment.getStartDateTime().plus(period) );
				}
				if (appointment.getEndDateTime() != null) {
					appointment.setEndDateTime( appointment.getEndDateTime().plus(period) );
				}
			}
			
			// if dragged from day to header
			else if ( (dragStartInDayHeader && dragEndInDayBody) ) {
				
				appointment.setWholeDay(false);

				// if this is a task
				if (appointment.getStartDateTime() != null && appointment.getEndDateTime() == null) {
					// set the drop time as the task time
					appointment.setStartDateTime(dragEndDateTime );
				}
				else {
					// simply add the duration, but without time
					Period period = Period.between(dragStartDateTime.toLocalDate(), dragEndDateTime.toLocalDate());
					appointment.setStartDateTime( appointment.getStartDateTime().toLocalDate().plus(period).atStartOfDay() );
					appointment.setEndDateTime( appointment.getEndDateTime().toLocalDate().plus(period).plusDays(1).atStartOfDay() );
				}
			}
			
			// redo whole week
			layoutHelp.skin.setupAppointments();					
		});
		
	}
	private Rectangle dragRectangle;
	private double startX = 0;
	private double startY = 0;
	private boolean mouseActuallyHasDragged = false;
	private final int roundToMinutes = 5;

	/**
	 * 
	 * @param localDateTime
	 * @return
	 */
	private boolean dragInDayBody(LocalDateTime localDateTime) {
		return localDateTime.getNano() == DRAG_DAY;
	}
	
	/**
	 * 
	 * @param localDateTime
	 * @return
	 */
	private boolean dragInDayHeader(LocalDateTime localDateTime) {
		return localDateTime.getNano() == DRAG_DAYHEADER;
	}
	static public final int DRAG_DAY = 1;
	static public final int DRAG_DAYHEADER = 0;
	
	/**
	 * 
	 */
	public String toString()
	{
		return "appointment=" + appointment.getStartDateTime() + "-" + appointment.getEndDateTime()
		     + ";"
			 + "sumary=" + appointment.getSummary()
			 ;
	}
}
