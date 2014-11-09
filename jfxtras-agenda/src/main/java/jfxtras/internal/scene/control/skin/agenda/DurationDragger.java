package jfxtras.internal.scene.control.skin.agenda;

import java.time.LocalDateTime;

import javafx.scene.Cursor;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import jfxtras.scene.control.agenda.Agenda.Appointment;
import jfxtras.util.NodeUtil;

public class DurationDragger extends Rectangle
{

	public DurationDragger(AppointmentRegularBodyPane appointmentPane, Appointment appointment, LayoutHelp layoutHelp)
	{
		// remember
		this.appointmentPane = appointmentPane;
		this.appointment = appointment;
		this.layoutHelp = layoutHelp;
		
		// bind: place at the bottom of the pane, 1/2 width of the pane, centered
		xProperty().bind( NodeUtil.snapXY(appointmentPane.widthProperty().multiply(0.25)) );
		yProperty().bind( NodeUtil.snapXY(appointmentPane.heightProperty().subtract(5)) );
		widthProperty().bind( appointmentPane.widthProperty().multiply(0.5) );
		setHeight(3);
		minimumHeight = (roundToMinutes * 60 * 1000) / layoutHelp.durationInMSPerPixelProperty.get();
		
		// styling
		getStyleClass().add("DurationDragger");

		// mouse
		layoutHelp.setupMouseOverAsBusy(this);
		setupMouseDrag();
	}
	private final AppointmentRegularBodyPane appointmentPane;
	private final Appointment appointment;
	private final LayoutHelp layoutHelp;
	private final int roundToMinutes = 5;
	private double minimumHeight = 5;
	
	private void setupMouseDrag() {
		// start resize
		setOnMousePressed( (mouseEvent) -> {
			
			// place a rectangle at exactly the same location as the appointmentPane
			DurationDragger.this.setCursor(Cursor.V_RESIZE);
			resizeRectangle = new Rectangle(appointmentPane.getLayoutX(), appointmentPane.getLayoutY(), appointmentPane.getWidth(), appointmentPane.getHeight()); // the values are already snapped
			resizeRectangle.getStyleClass().add("GhostRectangle");
			((DayBodyPane)appointmentPane.getParent()).getChildren().add(resizeRectangle);
			
			// place a text node at the bottom of the resize rectangle
			endTimeText = new Text(layoutHelp.timeDateTimeFormatter.format(appointment.getEndDateTime()));
			endTimeText.layoutXProperty().set(appointmentPane.getLayoutX()); 
			endTimeText.layoutYProperty().bind(resizeRectangle.heightProperty().add(appointmentPane.getLayoutY())); 
			endTimeText.getStyleClass().add("GhostRectangleText");
			((DayBodyPane)appointmentPane.getParent()).getChildren().add(endTimeText);

			// this event should not be processed by the appointment area
			mouseEvent.consume();
		});
		
		// visualize resize
		setOnMouseDragged( (mouseEvent) -> {
			
			//  calculate the number of pixels from on-screen nodeY (layoutY) to on-screen mouseY					
			double lNodeScreenY = NodeUtil.screenY(appointmentPane);
			double lMouseY = mouseEvent.getScreenY();
			double lHeight = lMouseY - lNodeScreenY;
			if (lHeight < minimumHeight) {
				lHeight = minimumHeight; // prevent underflow
			}
			resizeRectangle.setHeight( NodeUtil.snapWH(resizeRectangle.getLayoutY(), lHeight) );

			// show the current time in the label
			LocalDateTime endLocalDateTime = calculateEndDateTime();
			endTimeText.setText(layoutHelp.timeDateTimeFormatter.format(endLocalDateTime));

			// no one else
			mouseEvent.consume();
		});
		
		// end resize
		setOnMouseReleased( (mouseEvent) -> {			
			LocalDateTime endLocalDateTime = calculateEndDateTime();
							
			// reset ui
			DurationDragger.this.setCursor(Cursor.HAND);
			((DayBodyPane)appointmentPane.getParent()).getChildren().remove(resizeRectangle);
			resizeRectangle = null;					
			((DayBodyPane)appointmentPane.getParent()).getChildren().remove(endTimeText);
			endTimeText = null;
			
			// no one else
			mouseEvent.consume();
			
			// set the new enddate
			appointmentPane.appointment.setEndDateTime(endLocalDateTime);
			
			// relayout the entire skin
			layoutHelp.skin.setupAppointments();
		});
	}
	private Rectangle resizeRectangle;
	private Text endTimeText;
	
	private LocalDateTime calculateEndDateTime() {
		
		// calculate the new end datetime for the appointment (recalculating the duration)
		int ms = (int)(resizeRectangle.getHeight() * layoutHelp.durationInMSPerPixelProperty.get());
		LocalDateTime endLocalDateTime = appointmentPane.startDateTime.plusSeconds(ms / 1000);					
		
		// round to X minutes accuracy
		endLocalDateTime = layoutHelp.roundTimeToNearestMinutes(endLocalDateTime, roundToMinutes);
		return endLocalDateTime;
	}
}
