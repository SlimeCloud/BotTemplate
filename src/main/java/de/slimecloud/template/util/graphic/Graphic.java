package de.slimecloud.template.util.graphic;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
public abstract class Graphic {
	protected final int width;
	protected final int height;

	private final BufferedImage image;

	protected Graphic(int width, int height) {
		this.width = width;
		this.height = height;

		this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}

	protected void finish() {
		try {
			Graphics2D graphics = image.createGraphics();
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			drawGraphic(graphics);

			graphics.dispose();
		} catch (IOException e) {
			logger.error("Failed to render graphic", e);
		}
	}

	protected abstract void drawGraphic(@NotNull Graphics2D graphics) throws IOException;

	@NotNull
	public FileUpload getFile(@NotNull String name) {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", bos);

			return FileUpload.fromData(bos.toByteArray(), name);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@NotNull
	public FileUpload getFile() {
		return getFile("image.png");
	}
}
