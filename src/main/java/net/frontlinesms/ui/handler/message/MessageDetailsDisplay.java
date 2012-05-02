/**
 * 
 */
package net.frontlinesms.ui.handler.message;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import thinlet.Thinlet;

import net.frontlinesms.FrontlineUtils;
import net.frontlinesms.data.domain.FrontlineMessage;
import net.frontlinesms.data.domain.FrontlineMultimediaMessage;
import net.frontlinesms.data.domain.FrontlineMultimediaMessagePart;
import net.frontlinesms.messaging.mms.MmsUtils;
import net.frontlinesms.ui.FrontlineUiUtils;
import net.frontlinesms.ui.Icon;
import net.frontlinesms.ui.ThinletUiEventHandler;
import net.frontlinesms.ui.UiGeneratorController;
import net.frontlinesms.ui.i18n.InternationalisationUtils;

/**
 * @author Alex Anderson <alex@frontlinesms.com>
 */
public class MessageDetailsDisplay implements ThinletUiEventHandler {
	/** Path to the Thinlet XML layout file for the message details form */
	private static final String UI_FILE_MSG_DETAILS_FORM = "/ui/core/messages/dgMessageDetails.xml";

	private static final String I18N_MESSAGE_NO_CONTENT = "message.no.content";

	private static final String UI_COMPONENT_PN_CONTENT = "pnContent";
	private static final String UI_COMPONENT_PN_SUBJECT = "pnSubject";
	private static final String UI_COMPONENT_TF_CONTENT = "tfContent";
	private static final String UI_COMPONENT_TF_DATE = "tfDate";
	private static final String UI_COMPONENT_TF_SENDER = "tfSender";
	private static final String UI_COMPONENT_TF_RECIPIENT = "tfRecipient";
	private static final String UI_COMPONENT_TF_STATUS = "tfStatus";
	private static final String UI_COMPONENT_TF_SUBJECT = "tfSubject";

	private final UiGeneratorController ui;
	private Object dialog;

	private final String[] messageFileExtensions = { ".txt", ".htm", ".html" };
	
	public MessageDetailsDisplay(UiGeneratorController ui) {
		this.ui = ui;
	}

	public void show(FrontlineMessage message) {
		this.dialog = ui.loadComponentFromFile(UI_FILE_MSG_DETAILS_FORM, this);
		
		setText(UI_COMPONENT_TF_STATUS, InternationalisationUtils.getI18nString(message.getStatus()));
		setText(UI_COMPONENT_TF_SENDER, message.getSenderDisplayName() + " (" + message.getSenderMsisdn() + ")");
		setText(UI_COMPONENT_TF_RECIPIENT, message.getRecipientDisplayName() + " (" + message.getRecipientMsisdn() + ")");
		setText(UI_COMPONENT_TF_DATE, InternationalisationUtils.getDatetimeFormat().format(message.getDate()));
		
		if (message instanceof FrontlineMultimediaMessage && ((FrontlineMultimediaMessage) message).getSubject().length() > 0) {
			setText(UI_COMPONENT_TF_SUBJECT, ((FrontlineMultimediaMessage) message).getSubject());
			ui.setVisible(find(UI_COMPONENT_PN_SUBJECT), true);
		}
		
		Object contentPanel = find(UI_COMPONENT_PN_CONTENT);
		for(Object contentComponent : getContentComponents(message)) {
			ui.add(contentPanel, contentComponent);
		}
		
		ui.add(this.dialog);
	}

	private Object[] getContentComponents(FrontlineMessage message) {
		if(message instanceof FrontlineMultimediaMessage) {
			FrontlineMultimediaMessage mm = (FrontlineMultimediaMessage) message;
			List<FrontlineMultimediaMessagePart> parts = mm.getMultimediaParts();
			
			if(parts.size() == 0) {
				return new Object[]{ ui.createLabel(InternationalisationUtils.getI18nString(I18N_MESSAGE_NO_CONTENT)) }; // FIXME i18n
			} else {
				return getContentComponents(parts);
			}
		} else {
			// It's a standard text message
			Object textContent = ui.createTextarea(UI_COMPONENT_TF_CONTENT, message.getTextContent(), 8);
			this.ui.setWeight(textContent, 1, 1);
			this.ui.setEditable(textContent, false);
			
			return new Object[]{textContent}; 
		}
	}

	private Object[] getContentComponents(List<FrontlineMultimediaMessagePart> parts) {
		ArrayList<Object> contentComponents = new ArrayList<Object>(parts.size());
		for(FrontlineMultimediaMessagePart part : parts) {
			contentComponents.add(getComponent(part));
		}
		return contentComponents.toArray();
	}

	private Object getComponent(FrontlineMultimediaMessagePart part) {
		Object component;
		if(!part.isBinary()) {
			component = ui.createTextarea("", part.getTextContent(), (part.toString().length() + 44) / 45);
			this.ui.setEditable(component, false);
		} else {
			Object panel = ui.createPanel("");
			ui.setColumns(panel, 1);

			File mediaFile = MmsUtils.getFile(part);
			String openAction = "openMultimediaPart('" + part.getFilename() + "')";
			
			Image thumb = null;
			try {
				thumb = FrontlineUiUtils.getLimitedSizeImage(ImageIO.read(mediaFile), 64, 64);
			} catch(Exception ex) {}
			if(thumb != null) {
				Object thumbComponent = ui.createLink("", openAction, panel, this);
				ui.setIcon(thumbComponent, thumb);
				ui.add(panel, thumbComponent);
			}
			
			Object fileLink = ui.createLink("[ " + part.getFilename() + " ]", openAction, panel, this);
			if (isTextFile(part.getFilename())) {
				this.ui.setIcon(fileLink, Icon.SMS);
			}
			ui.add(panel, fileLink);
			component = panel;
		}
		ui.setWeight(component, 1, 1);
		return component;
	}
	
	private boolean isTextFile(String filename) {
		for (String textFileExtension : messageFileExtensions ) {
			if (filename.endsWith(textFileExtension)) {
				return true;
			}
		}
		
		return false;
	}

	public void openMultimediaPart(String filename) {
		FrontlineUtils.openExternalBrowser(MmsUtils.getAbsolutePath(filename));
	}

	/**
	 * Remove the supplied dialog from view.
	 * @param dialog the dialog to remove
	 * @see UiGeneratorController#removeDialog(Object)
	 */
	public void removeDialog(Object dialog) {
		this.ui.removeDialog(dialog);
	}
	
	private void setText(String componentName, String text) {
		ui.setText(find(componentName), text);
	}
	
	private Object find(String componentName) {
		return Thinlet.find(this.dialog, componentName);
	}
}
