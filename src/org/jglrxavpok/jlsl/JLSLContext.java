package org.jglrxavpok.jlsl;

import java.io.*;
import java.util.*;

import org.jglrxavpok.jlsl.fragments.*;

public class JLSLContext
{

	private CodeDecoder		   decoder;
	private CodeEncoder		   encoder;
	private ArrayList<CodeFilter> filters;

	public JLSLContext(CodeDecoder decoder, CodeEncoder encoder)
	{
		this.filters = new ArrayList<CodeFilter>();
		this.decoder = decoder;
		this.decoder.context = this;
		this.encoder = encoder;
		this.encoder.context = this;
	}

	public void addFilters(CodeFilter... filters)
	{
		for(CodeFilter filter : filters)
			this.filters.add(filter);
	}

	public void requestAnalysisForEncoder(Object data)
	{
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		ArrayList<CodeFragment> finalFragments = new ArrayList<CodeFragment>();
		CodeFragment[] array = (CodeFragment[])fragments.stream().map(fragment -> filter(fragment)).toArray();
		for(CodeFragment frag : array)
		{
			if(frag != null) finalFragments.add(frag);
		}
		// finish
		encoder.onRequestResult(finalFragments);
	}

	private CodeFragment filter(CodeFragment fragment)
	{
		for(CodeFilter filter : filters)
		{
			fragment = filter.filter(fragment);
		}
		return fragment;
	}

	public void execute(Object data, PrintWriter out)
	{
		ArrayList<CodeFragment> fragments = new ArrayList<CodeFragment>();
		decoder.handleClass(data, fragments);
		encoder.createSourceCode(fragments, out);
	}
}
