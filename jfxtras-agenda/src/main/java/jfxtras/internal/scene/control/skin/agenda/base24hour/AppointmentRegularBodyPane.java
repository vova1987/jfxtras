package jfxtras.internal.scene.control.skin.agenda.base24hour;

import java.time.LocalDate;

import javafx.scene.text.Text;
import jfxtras.scene.control.agenda.Agenda.Appointment;

public class AppointmentRegularBodyPane extends AppointmentAbstractTrackedPane {

	public AppointmentRegularBodyPane(LocalDate localDate, Appointment appointment, LayoutHelp layoutHelp) {
		super(localDate, appointment, layoutHelp);
		
		// strings
		this.startAsString = layoutHelp.timeDateTimeFormatter.format(this.startDateTime);
		this.endAsString = layoutHelp.timeDateTimeFormatter.format(this.endDateTime);

		// add the duration as text
		Text lTimeText = new Text((firstPaneOfAppointment ? startAsString : "") + "-" + (lastPaneOfAppointment ? endAsString : ""));
		{
			lTimeText.getStyleClass().add("AppointmentTimeLabel");
			lTimeText.setX( layoutHelp.paddingProperty.get() );
			lTimeText.setY(lTimeText.prefHeight(0));
			layoutHelp.clip(lTimeText, widthProperty().subtract( layoutHelp.paddingProperty.get() ), heightProperty());
			getChildren().add(lTimeText);
		}
		
		// add summary
		Text lSummaryText = new Text(appointment.getSummary());
		{
			lSummaryText.getStyleClass().add("AppointmentLabel");
			lSummaryText.setX( layoutHelp.paddingProperty.get() );
			lSummaryText.setY( lTimeText.getY() + layoutHelp.textHeightProperty.get());
			lSummaryText.wrappingWidthProperty().bind(widthProperty().subtract( layoutHelp.paddingProperty.get() ));
			layoutHelp.clip(lSummaryText, widthProperty(), heightProperty().subtract( layoutHelp.paddingProperty.get() ));
			getChildren().add(lSummaryText);			
		}
		
		// add the menu header
		getChildren().add(appointmentMenu);
		
		// add the duration dragger
		layoutHelp.skinnable.allowResizeProperty().addListener( (observable) -> {
			setupDurationDragger();
		});
		setupDurationDragger();
	}
	private String startAsString;
	private String endAsString;
	
	/**
	 * 
	 */
	private void setupDurationDragger() {
		if (lastPaneOfAppointment && layoutHelp.skinnable.getAllowResize()) {
			if (durationDragger == null) {
				durationDragger = new DurationDragger(this, appointment, layoutHelp);
			}
			getChildren().add(durationDragger);
		}
		else {
			getChildren().remove(durationDragger);
		}
	}
	private DurationDragger durationDragger = null;
}
